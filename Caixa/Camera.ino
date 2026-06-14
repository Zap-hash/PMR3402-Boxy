#include <Arduino.h>
#include <WiFi.h>
#include <WebServer.h>
#include <esp_camera.h>
#include <ESP_Mail_Client.h>

#define WIFI_SSID       "nome-do-hotspot"
#define WIFI_PASSWORD   "senha-do-hotspot"

#define EMAIL_REMETENTE  "seuemail@gmail.com"
#define EMAIL_SENHA      "sua-senha-gmail"
#define EMAIL_DESTINATARIO "seuemail@gmail.com"

// Pinos câmera AI-Thinker ESP-CAM
#define PWDN_GPIO_NUM     32
#define RESET_GPIO_NUM    -1
#define XCLK_GPIO_NUM      0
#define SIOD_GPIO_NUM     26
#define SIOC_GPIO_NUM     27
#define Y9_GPIO_NUM       35
#define Y8_GPIO_NUM       34
#define Y7_GPIO_NUM       39
#define Y6_GPIO_NUM       36
#define Y5_GPIO_NUM       21
#define Y4_GPIO_NUM       19
#define Y3_GPIO_NUM       18
#define Y2_GPIO_NUM        5
#define VSYNC_GPIO_NUM    25
#define HREF_GPIO_NUM     23
#define PCLK_GPIO_NUM     22

WebServer server(80);
SMTPSession smtp;

void initCamera() {
  camera_config_t config;
  config.ledc_channel = LEDC_CHANNEL_0;
  config.ledc_timer   = LEDC_TIMER_0;
  config.pin_d0       = Y2_GPIO_NUM;
  config.pin_d1       = Y3_GPIO_NUM;
  config.pin_d2       = Y4_GPIO_NUM;
  config.pin_d3       = Y5_GPIO_NUM;
  config.pin_d4       = Y6_GPIO_NUM;
  config.pin_d5       = Y7_GPIO_NUM;
  config.pin_d6       = Y8_GPIO_NUM;
  config.pin_d7       = Y9_GPIO_NUM;
  config.pin_xclk     = XCLK_GPIO_NUM;
  config.pin_pclk     = PCLK_GPIO_NUM;
  config.pin_vsync    = VSYNC_GPIO_NUM;
  config.pin_href     = HREF_GPIO_NUM;
  config.pin_sscb_sda = SIOD_GPIO_NUM;
  config.pin_sscb_scl = SIOC_GPIO_NUM;
  config.pin_pwdn     = PWDN_GPIO_NUM;
  config.pin_reset    = RESET_GPIO_NUM;
  config.xclk_freq_hz = 20000000;
  config.pixel_format = PIXFORMAT_JPEG;
  config.frame_size   = FRAMESIZE_VGA;
  config.jpeg_quality = 12;
  config.fb_count     = 1;

  esp_err_t err = esp_camera_init(&config);
  if (err != ESP_OK) {
    Serial.printf("Erro ao iniciar camera: 0x%x\n", err);
    return;
  }
  Serial.println("Camera iniciada.");
}

bool sendPhotoEmail(camera_fb_t* fb) {
  ESP_Mail_Session session;
  session.server.host_name = "smtp.gmail.com";
  session.server.port      = 465;
  session.login.email      = EMAIL_REMETENTE;
  session.login.password   = EMAIL_SENHA;
  session.login.user_domain = "";

  SMTP_Message message;
  message.sender.name  = "SmartBox Camera";
  message.sender.email = EMAIL_REMETENTE;
  message.subject      = "SmartBox - Imagem capturada";
  message.addRecipient("Destinatario", EMAIL_DESTINATARIO);
  message.text.content = "Imagem capturada pelo SmartBox em anexo.";

  // Anexa a foto
  SMTP_Attachment attachment;
  attachment.descr.filename    = "foto.jpg";
  attachment.descr.mime        = "image/jpeg";
  attachment.blob.data         = fb->buf;
  attachment.blob.size         = fb->len;
  attachment.transfer_encoding = Content_Transfer_Encoding::enc_base64;
  message.addAttachment(attachment);

  if (!smtp.connect(&session)) {
    Serial.println("Erro ao conectar SMTP: " + smtp.errorReason());
    return false;
  }

  if (!MailClient.sendMail(&smtp, &message)) {
    Serial.println("Erro ao enviar email: " + smtp.errorReason());
    return false;
  }

  Serial.println("Email enviado com sucesso!");
  return true;
}

void handleCapture() {
  Serial.println("Requisicao recebida - capturando foto...");

  camera_fb_t* fb = esp_camera_fb_get();
  if (!fb) {
    Serial.println("Erro ao capturar frame.");
    server.send(500, "text/plain", "erro_captura");
    return;
  }

  bool ok = sendPhotoEmail(fb);
  esp_camera_fb_return(fb);

  if (ok) {
    server.send(200, "text/plain", "ok");
  } else {
    server.send(500, "text/plain", "erro_email");
  }
}

void setup() {
  Serial.begin(115200);
  initCamera();

  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Conectando WiFi");
  while (WiFi.status() != WL_CONNECTED) {
    Serial.print(".");
    delay(300);
  }
  Serial.println("\nWiFi conectado!");
  Serial.println("IP do ESP-CAM camera: " + WiFi.localIP().toString());

  server.on("/capture", handleCapture);
  server.begin();
  Serial.println("Servidor HTTP iniciado.");
}

void loop() {
  server.handleClient();
}
