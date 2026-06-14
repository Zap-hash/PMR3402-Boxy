#include <Arduino.h>
#include <WiFi.h>
#include <FirebaseESP32.h>
#include <Keypad.h>
#include <LiquidCrystal.h>
#include <ESP32Servo.h>
#include <HTTPClient.h>
#include <time.h>

// ─────────────────────────────────────────
// Defines
// ─────────────────────────────────────────
#define FIREBASE_HOST      "seu-projeto-default-rtdb.firebaseio.com"
#define FIREBASE_API_KEY   "sua-api-key-do-projeto"
#define WIFI_SSID          "nome-do-hotspot"
#define WIFI_PASSWORD      "senha-do-hotspot"
#define FIREBASE_EMAIL     "seu-email@gmail.com"
#define FIREBASE_PASSWORD  "sua-senha-firebase"
#define ESPCAM_QR_IP       "192.168.x.x"
#define ESPCAM_CAMERA_IP   "192.168.x.x"

#define EVENT_QUEUE_SIZE   10
#define PIN_BUFFER_SIZE    5
#define MAX_TENTATIVAS     5

// ─────────────────────────────────────────
// Enums
// ─────────────────────────────────────────
enum StateEnum {
  STATE_IDLE = 0,
  STATE_AUTENTICANDO,
  STATE_ABERTO,
  STATE_REGISTRANDO,
  NUM_STATES
};

enum EventType {
  EVT_TECLA = 0,
  EVT_TECLA_ENTER,
  EVT_QRCODE_LIDO,
  EVT_HORARIO_ATINGIDO,
  EVT_TAMPA_ABERTA,
  EVT_TAMPA_FECHADA,
  EVT_AUTH_OK,
  EVT_AUTH_FAIL,
  EVT_TIMEOUT,
  EVT_DB_OK,
  EVT_DB_FAIL,
  NUM_EVENTS
};

// ─────────────────────────────────────────
// Struct Event
// ─────────────────────────────────────────
struct Event {
  EventType type;
  char      data[32];
};

// ─────────────────────────────────────────
// Fila global de eventos (FreeRTOS)
// ─────────────────────────────────────────
QueueHandle_t eventQueue;

// ─────────────────────────────────────────
// Classe FirebaseDrive
// ─────────────────────────────────────────
class FirebaseDrive {
private:
  FirebaseData   _fbdo;
  FirebaseAuth   _auth;
  FirebaseConfig _config;

public:
  void init() {
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
    Serial.print("Conectando WiFi");
    while (WiFi.status() != WL_CONNECTED) {
      Serial.print(".");
      delay(300);
    }
    Serial.println("\nWiFi conectado: " + WiFi.localIP().toString());

    _config.api_key      = FIREBASE_API_KEY;
    _config.database_url = FIREBASE_HOST;
    _auth.user.email     = FIREBASE_EMAIL;
    _auth.user.password  = FIREBASE_PASSWORD;

    Firebase.begin(&_config, &_auth);
    Firebase.reconnectWiFi(true);

    Serial.print("Autenticando no Firebase");
    uint32_t timeout = millis();
    while (!Firebase.ready()) {
      if (millis() - timeout > 10000) {
        Serial.println("\nTimeout Firebase.");
        return;
      }
      Serial.print(".");
      delay(300);
    }
    Serial.println("\nFirebase pronto.");
  }

  // retorna "event_code_valido" | "event_code_ja_utilizado" | "event_code_invalido"
  String validateCode(const char* code) {
    if (!Firebase.ready()) return "event_code_invalido";
    String path = "/packages/" + String(code) + "/status";
    if (Firebase.getString(_fbdo, path)) {
      String status = _fbdo.stringData();
      if (status == "pendente") return "event_code_valido";
      return "event_code_ja_utilizado";
    }
    Serial.println("Erro Firebase: " + _fbdo.errorReason());
    return "event_code_invalido";
  }

  String getCompartimento(const char* code) {
    if (!Firebase.ready()) return "";
    String path = "/packages/" + String(code) + "/compartimento";
    if (Firebase.getString(_fbdo, path)) return _fbdo.stringData();
    return "";
  }

  // retorna "event_delivery_ok" | "event_delivery_erro"
  String registerDelivery(const char* code) {
    if (!Firebase.ready()) return "event_delivery_erro";
    String path = "/packages/" + String(code) + "/status";
    if (Firebase.setString(_fbdo, path, "entregue")) return "event_delivery_ok";
    Serial.println("Erro Firebase: " + _fbdo.errorReason());
    return "event_delivery_erro";
  }

  // verifica horario agendado no firebase
  bool checkSchedule(int currentMinutes) {
    if (!Firebase.ready()) return false;
    // busca schedules do usuario
    if (Firebase.getJSON(_fbdo, "/schedules")) {
      // simplificado: retorna true se currentMinutes esta dentro de alguma janela
      // implementacao completa depende da estrutura do schedule
      return false;
    }
    return false;
  }
};

// ─────────────────────────────────────────
// Classe DoorSensorDrive
// ─────────────────────────────────────────
class DoorSensorDrive {
private:
  uint8_t _pin;

public:
  DoorSensorDrive(uint8_t pin) : _pin(pin) {}

  void init() {
    pinMode(_pin, INPUT_PULLUP);
    attachInterruptArg(
      digitalPinToInterrupt(_pin),
      [](void* arg) {
        DoorSensorDrive* self = (DoorSensorDrive*)arg;
        self->isr_handler();
      },
      this,
      CHANGE
    );
  }

  void IRAM_ATTR isr_handler() {
    Event evt;
    evt.type = (digitalRead(_pin) == HIGH)
      ? EVT_TAMPA_ABERTA
      : EVT_TAMPA_FECHADA;
    evt.data[0] = '\0';
    BaseType_t xHigherPriorityTaskWoken = pdFALSE;
    xQueueSendFromISR(eventQueue, &evt, &xHigherPriorityTaskWoken);
    portYIELD_FROM_ISR(xHigherPriorityTaskWoken);
  }
};

// ─────────────────────────────────────────
// Classe MatrixKeypadDrive
// ─────────────────────────────────────────
class MatrixKeypadDrive {
private:
  static const uint8_t ROWS = 4;
  static const uint8_t COLS = 4;

  char _keys[4][4] = {
    {'1', '2', '3', 'A'},
    {'4', '5', '6', 'B'},
    {'7', '8', '9', 'C'},
    {'*', '0', '#', 'D'}
  };

  uint8_t _rowPins[4];
  uint8_t _colPins[4];
  Keypad* _keypad;

public:
  MatrixKeypadDrive(uint8_t rowPins[4], uint8_t colPins[4]) {
    memcpy(_rowPins, rowPins, 4);
    memcpy(_colPins, colPins, 4);
  }

  void init() {
    _keypad = new Keypad(makeKeymap(_keys), _rowPins, _colPins, ROWS, COLS);
  }

  // chamado pela task do teclado
  void poll() {
    char key = _keypad->getKey();
    if (key == NO_KEY) return;

    Event evt;
    evt.data[0] = key;
    evt.data[1] = '\0';

    if (key == '#') {
      evt.type = EVT_TECLA_ENTER;
    } else if (key >= '0' && key <= '9') {
      evt.type = EVT_TECLA;
    } else {
      return;  // ignora A, B, C, D, *
    }

    xQueueSend(eventQueue, &evt, 0);
  }
};

// ─────────────────────────────────────────
// Classe TimeoutManager
// ─────────────────────────────────────────
class TimeoutManager {
private:
  TimerHandle_t _timer;

  static void _callback(TimerHandle_t xTimer) {
    Event evt;
    evt.type    = EVT_TIMEOUT;
    evt.data[0] = '\0';
    xQueueSend(eventQueue, &evt, 0);
  }

public:
  void init() {
    _timer = xTimerCreate(
      "timeout",
      pdMS_TO_TICKS(10000),
      pdFALSE,   // one-shot
      nullptr,
      _callback
    );
  }

  void start(uint32_t ms) {
    xTimerChangePeriod(_timer, pdMS_TO_TICKS(ms), 0);
    xTimerStart(_timer, 0);
  }

  void stop() {
    xTimerStop(_timer, 0);
  }
};

// ─────────────────────────────────────────
// Classe RTCDriver
// ─────────────────────────────────────────
class RTCDriver {
private:
  const char* _ntpServer      = "pool.ntp.org";
  const long  _gmtOffset      = -3 * 3600;
  const int   _daylightOffset = 0;

public:
  void init() {
    configTime(_gmtOffset, _daylightOffset, _ntpServer);
    struct tm timeinfo;
    uint32_t timeout = millis();
    while (!getLocalTime(&timeinfo)) {
      if (millis() - timeout > 10000) {
        Serial.println("Timeout NTP.");
        return;
      }
      delay(500);
    }
    Serial.println("Horario sincronizado.");
  }

  int get_current_time() {
    struct tm timeinfo;
    if (!getLocalTime(&timeinfo)) return -1;
    return (timeinfo.tm_hour * 60) + timeinfo.tm_min;
  }

  // chamado pela task do RTC
  void poll(int scheduleStart, int scheduleEnd) {
    int now = get_current_time();
    if (now < 0) return;

    static bool dentroJanela = false;

    if (!dentroJanela && now >= scheduleStart && now < scheduleEnd) {
      dentroJanela = true;
      Event evt;
      evt.type    = EVT_HORARIO_ATINGIDO;
      evt.data[0] = '\0';
      xQueueSend(eventQueue, &evt, 0);
    }

    if (dentroJanela && now >= scheduleEnd) {
      dentroJanela = false;
    }
  }
};

// ─────────────────────────────────────────
// Classe DisplayDriver
// ─────────────────────────────────────────
class DisplayDriver {
private:
  LiquidCrystal* _lcd;
  uint8_t _cols;
  uint8_t _rows;

public:
  DisplayDriver(uint8_t rs, uint8_t en,
                uint8_t d4, uint8_t d5, uint8_t d6, uint8_t d7,
                uint8_t cols = 16, uint8_t rows = 2) {
    _cols = cols;
    _rows = rows;
    _lcd  = new LiquidCrystal(rs, en, d4, d5, d6, d7);
  }

  void init() {
    _lcd->begin(_cols, _rows);
    _lcd->clear();
  }

  void clear() { _lcd->clear(); }

  void show(const char* msg, uint8_t col = 0, uint8_t row = 0) {
    _lcd->setCursor(col, row);
    _lcd->print(msg);
  }

  void show_erro(const char* msg) {
    _lcd->clear();
    _lcd->setCursor(0, 0);
    _lcd->print("    ERRO!");
    _lcd->setCursor(0, 1);
    _lcd->print(String(msg).substring(0, 16).c_str());
  }

  void show_sucesso(const char* msg) {
    _lcd->clear();
    _lcd->setCursor(0, 0);
    _lcd->print("   SUCESSO!");
    _lcd->setCursor(0, 1);
    _lcd->print(String(msg).substring(0, 16).c_str());
  }

  void update_buffer(const char* buffer) {
    _lcd->setCursor(0, 1);
    _lcd->print("PIN: ");
    _lcd->print(buffer);
  }
};

// ─────────────────────────────────────────
// Classe LockDriver
// ─────────────────────────────────────────
class LockDriver {
private:
  Servo   _servo;
  uint8_t _pin;
  bool    _locked;

public:
  LockDriver(uint8_t pin) : _pin(pin), _locked(true) {}

  void init() {
    _servo.attach(_pin);
    _servo.write(90);  // inicia travado
    _locked = true;
  }

  void lock() {
    _servo.write(90);
    _locked = true;
    Serial.println("Fechadura TRAVADA");
  }

  void unlock() {
    _servo.write(0);
    _locked = false;
    Serial.println("Fechadura ABERTA");
  }

  bool is_locked() { return _locked; }
};

// ─────────────────────────────────────────
// Classe QRreaderDriver
// ─────────────────────────────────────────
class QRreaderDriver {
private:
  String _url;

public:
  void init() {
    _url = "http://" + String(ESPCAM_QR_IP) + "/scan";
  }

  // chamado pela task do QR
  void poll() {
    if (WiFi.status() != WL_CONNECTED) return;

    HTTPClient http;
    http.begin(_url);
    http.setTimeout(3000);
    int httpCode = http.GET();

    if (httpCode == 200) {
      String payload = http.getString();
      payload.trim();
      if (payload != "-1" && payload.length() > 0) {
        Event evt;
        evt.type = EVT_QRCODE_LIDO;
        strncpy(evt.data, payload.c_str(), 31);
        evt.data[31] = '\0';
        xQueueSend(eventQueue, &evt, 0);
      }
    }
    http.end();
  }
};

// ─────────────────────────────────────────
// Classe CameraDriver
// ─────────────────────────────────────────
class CameraDriver {
private:
  String _url;

public:
  void init() {
    _url = "http://" + String(ESPCAM_CAMERA_IP) + "/capture";
  }

  String captureImage() {
    if (WiFi.status() != WL_CONNECTED) return "event_capture_erro";
    HTTPClient http;
    http.begin(_url);
    http.setTimeout(10000);
    int httpCode = http.GET();
    http.end();
    return (httpCode == 200) ? "event_capture_ok" : "event_capture_erro";
  }
};

// ─────────────────────────────────────────
// Instâncias globais
// ─────────────────────────────────────────
uint8_t rowPins[4] = {15, 4, 16, 17};
uint8_t colPins[4] = {5, 18, 19, 22};

FirebaseDrive     firebase;
DoorSensorDrive   doorSensor(2);
MatrixKeypadDrive keypad(rowPins, colPins);
TimeoutManager    timeoutMgr;
RTCDriver         rtc;
DisplayDriver     display(13, 12, 14, 27, 26, 25);
LockDriver        lockInferior(21);
LockDriver        lockSuperior(23);
QRreaderDriver    qrReader;
CameraDriver      camera;

// ─────────────────────────────────────────
// Classe StateMachine
// ─────────────────────────────────────────
class StateMachine {
private:

  // ── Contexto ──────────────────────────
  StateEnum _state;
  char      _pinBuffer[PIN_BUFFER_SIZE];
  uint8_t   _bufferIdx;
  bool      _tampaFoiAberta;
  uint8_t   _tentativas;
  char      _lastCode[32];  // ultimo PIN ou QR validado

  // ── Tipo ponteiro de ação ──────────────
  typedef void (StateMachine::*ActionFn)();

  // ── Tipo ponteiro de guard ─────────────
  typedef bool (StateMachine::*GuardFn)();

  // ── Estrutura de transição ─────────────
  struct Transition {
    StateEnum nextState;
    ActionFn  actions[4];
    uint8_t   numActions;
    GuardFn   guard;        // nullptr = sem guard
  };

  // ── Matriz de transições ───────────────
  // [estado_atual][evento] → Transition
  Transition _table[NUM_STATES][NUM_EVENTS];

  // ─────────────────────────────────────
  // GUARDS
  // ─────────────────────────────────────
  bool guard_bufferLen4() {
    return _bufferIdx == 4;
  }

  bool guard_bufferNotLen4() {
    return _bufferIdx != 4;
  }

  bool guard_tampaFoiAberta() {
    return _tampaFoiAberta == true;
  }

  bool guard_tampaNotAberta() {
    return _tampaFoiAberta == false;
  }

  bool guard_tentativasOk() {
    return _tentativas < MAX_TENTATIVAS;
  }

  bool guard_tentativasMax() {
    return _tentativas >= MAX_TENTATIVAS;
  }

  // ─────────────────────────────────────
  // AÇÕES
  // ─────────────────────────────────────

  // A1 - adiciona char ao buffer
  void a1_bufferPush() {
    if (_bufferIdx < 4) {
      _pinBuffer[_bufferIdx++] = _currentEvent.data[0];
      _pinBuffer[_bufferIdx]   = '\0';
    }
  }

  // A2 - atualiza display com buffer
  void a2_displayUpdate() {
    display.update_buffer(_pinBuffer);
  }

  // A3 - limpa buffer
  void a3_bufferClear() {
    memset(_pinBuffer, 0, sizeof(_pinBuffer));
    _bufferIdx = 0;
  }

  // A4 - display erro
  void a4_displayError() {
    display.show_erro("Codigo invalido");
  }

  // A5 - valida PIN no firebase (envia evento AUTH_OK ou AUTH_FAIL)
  void a5_validatePin() {
    strncpy(_lastCode, _pinBuffer, 31);
    // roda em task separada para nao bloquear
    xTaskCreate(
      [](void* arg) {
        StateMachine* self = (StateMachine*)arg;
        String result = firebase.validateCode(self->_lastCode);
        Event evt;
        evt.type = (result == "event_code_valido") ? EVT_AUTH_OK : EVT_AUTH_FAIL;
        evt.data[0] = '\0';
        xQueueSend(eventQueue, &evt, portMAX_DELAY);
        vTaskDelete(nullptr);
      },
      "validate_pin",
      4096,
      this,
      1,
      nullptr
    );
  }

  // A6 - inicia timeout
  void a6_timeoutStart() {
    timeoutMgr.start(10000);  // 10s para autenticação
  }

  // A7 - valida QR no firebase
  void a7_validateQR() {
    strncpy(_lastCode, _currentEvent.data, 31);
    xTaskCreate(
      [](void* arg) {
        StateMachine* self = (StateMachine*)arg;
        String result = firebase.validateCode(self->_lastCode);
        Event evt;
        evt.type = (result == "event_code_valido") ? EVT_AUTH_OK : EVT_AUTH_FAIL;
        evt.data[0] = '\0';
        xQueueSend(eventQueue, &evt, portMAX_DELAY);
        vTaskDelete(nullptr);
      },
      "validate_qr",
      4096,
      this,
      1,
      nullptr
    );
  }

  // A8 - destrava fechadura
  void a8_lockUnlock() {
    String comp = firebase.getCompartimento(_lastCode);
    if (comp == "superior") {
      lockSuperior.unlock();
    } else {
      lockInferior.unlock();
    }
    display.show("Deposite aqui!", 0, 0);
    display.show(comp.c_str(), 0, 1);
  }

  // A9 - trava fechadura
  void a9_lockLock() {
    lockSuperior.lock();
    lockInferior.lock();
  }

  // A10 - limpa buffer e para timeout
  void a10_clearAndStopTimeout() {
    memset(_pinBuffer, 0, sizeof(_pinBuffer));
    _bufferIdx = 0;
    timeoutMgr.stop();
  }

  // A11 - captura foto
  void a11_cameraCapture() {
    xTaskCreate(
      [](void* arg) {
        camera.captureImage();
        vTaskDelete(nullptr);
      },
      "capture",
      4096,
      nullptr,
      1,
      nullptr
    );
  }

  // A12 - seta flag tampa aberta e para timeout
  void a12_setTampaFlag() {
    _tampaFoiAberta = true;
    timeoutMgr.stop();
  }

  // A13 - reseta flag tampa
  void a13_resetTampaFlag() {
    _tampaFoiAberta = false;
  }

  // A14 - display sucesso e reseta tentativas
  void a14_successAndReset() {
    display.show_sucesso("Entregue!");
    _tentativas = 0;
  }

  // A15 - registra entrega e incrementa tentativas
  void a15_registerDelivery() {
    _tentativas++;
    xTaskCreate(
      [](void* arg) {
        StateMachine* self = (StateMachine*)arg;
        String result = firebase.registerDelivery(self->_lastCode);
        Event evt;
        evt.type    = (result == "event_delivery_ok") ? EVT_DB_OK : EVT_DB_FAIL;
        evt.data[0] = '\0';
        xQueueSend(eventQueue, &evt, portMAX_DELAY);
        vTaskDelete(nullptr);
      },
      "register",
      4096,
      this,
      1,
      nullptr
    );
  }

  // A16 - loga erro e reseta tentativas
  void a16_logErrorAndReset() {
    Serial.println("ERRO: Max tentativas atingido. Entrega nao registrada.");
    display.show_erro("Erro registro");
    _tentativas = 0;
  }

  // ─────────────────────────────────────
  // Evento atual (para acoes acessarem)
  // ─────────────────────────────────────
  Event _currentEvent;

  // ─────────────────────────────────────
  // Inicializa tabela de transições
  // ─────────────────────────────────────
  void _initTable() {
    // zera tabela
    memset(_table, 0, sizeof(_table));

    // Helper lambda para preencher entrada
    auto set = [&](StateEnum s, EventType e,
                   StateEnum next,
                   std::initializer_list<ActionFn> acts,
                   GuardFn guard = nullptr) {
      _table[s][e].nextState  = next;
      _table[s][e].guard      = guard;
      _table[s][e].numActions = 0;
      for (auto a : acts) {
        _table[s][e].actions[_table[s][e].numActions++] = a;
      }
    };

    // ── IDLE ──────────────────────────────────────────────────────
    set(STATE_IDLE, EVT_TECLA,
        STATE_IDLE,
        {&StateMachine::a1_bufferPush, &StateMachine::a2_displayUpdate});

    set(STATE_IDLE, EVT_TECLA_ENTER,
        STATE_AUTENTICANDO,
        {&StateMachine::a5_validatePin, &StateMachine::a6_timeoutStart},
        &StateMachine::guard_bufferLen4);

    set(STATE_IDLE, EVT_TECLA_ENTER,
        STATE_IDLE,
        {&StateMachine::a4_displayError, &StateMachine::a3_bufferClear},
        &StateMachine::guard_bufferNotLen4);

    set(STATE_IDLE, EVT_QRCODE_LIDO,
        STATE_AUTENTICANDO,
        {&StateMachine::a7_validateQR, &StateMachine::a6_timeoutStart});

    set(STATE_IDLE, EVT_HORARIO_ATINGIDO,
        STATE_ABERTO,
        {&StateMachine::a8_lockUnlock});

    // ── AUTENTICANDO ───────────────────────────────────────────────
    set(STATE_AUTENTICANDO, EVT_AUTH_OK,
        STATE_ABERTO,
        {&StateMachine::a8_lockUnlock, &StateMachine::a10_clearAndStopTimeout});

    set(STATE_AUTENTICANDO, EVT_AUTH_FAIL,
        STATE_IDLE,
        {&StateMachine::a4_displayError, &StateMachine::a10_clearAndStopTimeout});

    set(STATE_AUTENTICANDO, EVT_TIMEOUT,
        STATE_IDLE,
        {&StateMachine::a4_displayError, &StateMachine::a3_bufferClear});

    // ── ABERTO ─────────────────────────────────────────────────────
    set(STATE_ABERTO, EVT_TAMPA_ABERTA,
        STATE_ABERTO,
        {&StateMachine::a12_setTampaFlag});

    set(STATE_ABERTO, EVT_TAMPA_FECHADA,
        STATE_REGISTRANDO,
        {&StateMachine::a9_lockLock, &StateMachine::a11_cameraCapture},
        &StateMachine::guard_tampaFoiAberta);

    set(STATE_ABERTO, EVT_TAMPA_FECHADA,
        STATE_ABERTO,
        {},
        &StateMachine::guard_tampaNotAberta);

    set(STATE_ABERTO, EVT_TIMEOUT,
        STATE_IDLE,
        {&StateMachine::a9_lockLock, &StateMachine::a13_resetTampaFlag});

    // ── REGISTRANDO ────────────────────────────────────────────────
    set(STATE_REGISTRANDO, EVT_DB_OK,
        STATE_IDLE,
        {&StateMachine::a14_successAndReset});

    set(STATE_REGISTRANDO, EVT_DB_FAIL,
        STATE_REGISTRANDO,
        {&StateMachine::a15_registerDelivery},
        &StateMachine::guard_tentativasOk);

    set(STATE_REGISTRANDO, EVT_DB_FAIL,
        STATE_IDLE,
        {&StateMachine::a16_logErrorAndReset},
        &StateMachine::guard_tentativasMax);
  }

public:
  StateMachine() :
    _state(STATE_IDLE),
    _bufferIdx(0),
    _tampaFoiAberta(false),
    _tentativas(0) {
    memset(_pinBuffer, 0, sizeof(_pinBuffer));
    memset(_lastCode,  0, sizeof(_lastCode));
  }

  void init() {
    _initTable();
    _state = STATE_IDLE;
    display.show("Smart Box", 0, 0);
    display.show("Aguardando...", 0, 1);
    Serial.println("StateMachine iniciada.");
  }

  void process_event(Event& evt) {
    _currentEvent = evt;
    Transition& t = _table[_state][evt.type];

    // verifica guard
    if (t.guard != nullptr) {
      if (!(this->*t.guard)()) {
        // guard falhou, procura transição alternativa
        // (mesmo estado/evento com guard diferente ja foi mapeado
        //  na tabela como entrada separada pelo mesmo EVT — ver nota abaixo)
        return;
      }
    }

    // executa ações
    for (uint8_t i = 0; i < t.numActions; i++) {
      (this->*t.actions[i])();
    }

    // transição de estado
    if (t.nextState != _state) {
      Serial.printf("Estado: %d → %d\n", _state, t.nextState);
      _state = t.nextState;
    }
  }

  // task FreeRTOS principal
  void run() {
    Event evt;
    while (true) {
      if (xQueueReceive(eventQueue, &evt, portMAX_DELAY) == pdTRUE) {
        process_event(evt);
      }
    }
  }

  StateEnum getState() { return _state; }
};

// ─────────────────────────────────────────
// Instância da StateMachine
// ─────────────────────────────────────────
StateMachine sm;

// ─────────────────────────────────────────
// Tasks FreeRTOS
// ─────────────────────────────────────────

// Task principal — consome fila e roda máquina de estados
void task_state_machine(void* arg) {
  sm.run();
}

// Task teclado — polling a cada 50ms
void task_keypad(void* arg) {
  while (true) {
    keypad.poll();
    vTaskDelay(pdMS_TO_TICKS(50));
  }
}

// Task QR reader — polling a cada 500ms
void task_qr(void* arg) {
  while (true) {
    qrReader.poll();
    vTaskDelay(pdMS_TO_TICKS(500));
  }
}

// Task RTC — verifica horário a cada 30s
void task_rtc(void* arg) {
  while (true) {
    rtc.poll(570, 1170);  // 09:30 às 19:30 — ajuste conforme Firebase
    vTaskDelay(pdMS_TO_TICKS(30000));
  }
}

// ─────────────────────────────────────────
// Setup
// ─────────────────────────────────────────
void setup() {
  Serial.begin(115200);

  // cria fila de eventos
  eventQueue = xQueueCreate(EVENT_QUEUE_SIZE, sizeof(Event));
  if (eventQueue == nullptr) {
    Serial.println("ERRO: falha ao criar fila de eventos!");
    while (true);
  }

  // inicia drivers
  firebase.init();
  rtc.init();
  display.init();
  doorSensor.init();
  keypad.init();
  timeoutMgr.init();
  lockInferior.init();
  lockSuperior.init();
  qrReader.init();
  camera.init();

  // inicia máquina de estados
  sm.init();

  // cria tasks FreeRTOS
  xTaskCreate(task_state_machine, "sm_task",    8192, nullptr, 3, nullptr);
  xTaskCreate(task_keypad,        "keypad_task", 2048, nullptr, 2, nullptr);
  xTaskCreate(task_qr,            "qr_task",     4096, nullptr, 1, nullptr);
  xTaskCreate(task_rtc,           "rtc_task",    4096, nullptr, 1, nullptr);
}

// ─────────────────────────────────────────
// Loop — vazio, tudo roda nas tasks
// ─────────────────────────────────────────
void loop() {
  vTaskDelay(portMAX_DELAY);
}
