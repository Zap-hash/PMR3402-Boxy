#include <Arduino.h>
#include <WiFi.h>
#include <WebServer.h>
#include <esp_camera.h>
#include "quirc.h"

#define WIFI_SSID     "nome-do-hotspot"
#define WIFI_PASSWORD "senha-do-hotspot"

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
  config.pixel_format = PIXFORMAT_GRAYSCALE;
  config.frame_size   = FRAMESIZE_QVGA;
  config.fb_count     = 1;

  esp_err_t err = esp_camera_init(&config);
  if (err != ESP_OK) {
    Serial.printf("Erro ao iniciar camera: 0x%x\n", err);
    return;
  }
  Serial.println("Camera iniciada.");
}

String readQRFromCamera() {
  camera_fb_t* fb = esp_camera_fb_get();
  if (!fb) {
    Serial.println("Erro ao capturar frame.");
    return "-1";
  }

  struct quirc* q = quirc_new();
  if (!q) {
    esp_camera_fb_return(fb);
    return "-1";
  }

  quirc_resize(q, fb->width, fb->height);
  uint8_t* image = quirc_begin(q, NULL, NULL);
  memcpy(image, fb->buf, fb->len);
  quirc_end(q);

  int count = quirc_count(q);
  String result = "-1";

  if (count > 0) {
    struct quirc_code code;
    struct quirc_data data;
    quirc_extract(q, 0, &code);

    if (quirc_decode(&code, &data) == QUIRC_SUCCESS) {
      result = String((char*)data.payload);
      Serial.println("QR lido: " + result);
    }
  }

  quirc_destroy(q);
  esp_camera_fb_return(fb);
  return result;
}

void handleScan() {
  Serial.println("Requisicao recebida - lendo QR...");
  String qrcode = readQRFromCamera();
  server.send(200, "text/plain", qrcode);
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
  Serial.println("IP do ESP-CAM: " + WiFi.localIP().toString());

  server.on("/scan", handleScan);
  server.begin();
  Serial.println("Servidor HTTP iniciado.");
}

void loop() {
  server.handleClient();
}
