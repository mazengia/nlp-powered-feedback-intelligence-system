
# 📊 Intelli Feedback Insights Platform

> An AI-powered Customer Feedback Intelligence System built with **Spring Boot + Python Flask NLP microservices**, enabling real-time sentiment analysis, topic detection, keyphrase extraction, and business insights generation.

---

## Overview

The **Intelli Feedback Insights Platform** is a distributed AI system that analyzes customer feedback in real time. It combines a **Spring Boot backend (enterprise layer)** with a **Flask-based NLP microservice (AI layer)** exposed via REST APIs and connected using HTTP client integration.

The system is designed to simulate real-world production architecture used in modern AI-driven analytics platforms.

---

## Key Features

* 🔍 **Sentiment Analysis** (Positive / Negative / Neutral)
* 🏷️ **Topic Classification** (Auto-detect feedback category)
* 🔑 **Keyphrase Extraction** (Important keywords from text)
* 📝 **Automatic Text Summarization**
* 📊 **Batch Feedback Processing**
* 📈 **Business Insights Generation**
* 🌍 **Multilingual Support (including Amharic text handling)**
* ⚡ **Real-time REST API communication**

---

## 🏗️ System Architecture

```
Customer Feedback → Spring Boot API Layer → NLP Flask Microservice → AI Processing → JSON Response → Database Storage → Insights API
```

---

## ⚙️ Tech Stack

### Backend (Core System)

* Java 17
* Spring Boot
* Spring Data JPA
* H2 Database (dev environment)
* Java HttpClient
* Lombok

### NLP Microservice

* Python 3
* Flask
* JSON-based REST APIs
* (Extendable to spaCy / Transformers)

### Integration

* REST APIs
* Ngrok tunneling (development)
* Jackson JSON processing

---

## 📡 API Endpoints

### 🔹 Analyze Single Feedback

```
POST /api/analyze
```

### 🔹 Batch Analysis

```
POST /api/analyze/batch
```

### 🔹 Sentiment Analysis

```
POST /api/sentiment
```

### 🔹 Topic Classification

```
POST /api/topics
```

### 🔹 Keyphrase Extraction

```
POST /api/keyphrases
```

### 🔹 Summarization

```
POST /api/summarize
```

### 🔹 Insights Dashboard

```
POST /api/insights
```

### 🔹 Health Check

```
GET /health
```

---

## 📦 Example Request

```json
{
  "text": "The delivery was delayed and the product arrived damaged.",
  "feedback_type": "product_review",
  "source": "website"
}
```

---

## 📤 Example Response

```json
{
  "sentiment": {
    "sentiment": "negative",
    "emoji": "😡"
  },
  "topic": {
    "primary_topic": "delivery_issue"
  },
  "keyphrases": {
    "keyphrases": [
      {"phrase": "delivery delayed"},
      {"phrase": "product damaged"}
    ]
  },
  "summary": {
    "summary": "Customer is disappointed due to delayed delivery and damaged product."
  }
}
```

---

## 🧪 Batch Processing Example

The system supports analyzing multiple feedback records in a single request for scalable processing and analytics.

---

## 📊 Business Value

This system can be used for:

* Customer experience monitoring
* E-commerce feedback analysis
* Support ticket prioritization
* Product improvement insights
* Sentiment trend tracking

---

## 🔗 Project Architecture (High Level)

```
Frontend / API Client
        ↓
Spring Boot REST API
        ↓
NLP Service Client (HttpClient)
        ↓
Flask NLP Microservice
        ↓
AI Processing Layer
        ↓
JSON Response → Database → Insights Engine
```

---

## ⚡ How It Works

1. User submits feedback via REST API
2. Spring Boot stores and processes request
3. Request is sent to Flask NLP microservice
4. NLP engine analyzes sentiment, topic, keywords
5. Results returned to Spring Boot
6. Data is stored and enriched with insights
7. Aggregated analytics are generated

---
 

## 💻 Author

**Maze Tesfa**

* Passionate about AI, backend systems, and scalable architecture
* Focused on NLP and distributed systems

---

## 📌 Status

🚧 Development Project (MVP Complete)
⚡ Actively improving NLP capabilities
 
