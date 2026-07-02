# 🏛️ Civic Core
**AI-Powered Decision Intelligence Platform for Smart Cities & Public Services**

*Built for the Google Cloud Gen AI Academy (APAC Edition) - Challenge Track 1*

## 📌 Overview
Civic Core is an autonomous, multimodal AI triage agent designed to modernize city infrastructure management. It eliminates the need for complex, manual citizen reporting forms and prevents city officials from drowning in unstructured emails.

By allowing citizens to simply upload photos of hazards (e.g., a severe pothole or downed power line) along with a brief description, the system leverages Google's **Gemini 2.5 Flash** to visually analyze the damage, categorize it, and instantly assign a severity risk score. This structured JSON data is routed directly into a **Google BigQuery** data lake, which automatically populates a live command center dashboard for city dispatchers in milliseconds.

## ✨ Key Features
* **Multimodal Hazard Intake:** Processes both text descriptions and multiple image uploads simultaneously.
* **Automated AI Risk Scoring:** Evaluates infrastructure severity on a 1-10 scale based on visual evidence, removing human bias.
* **Cloud-Native Data Ingestion:** Securely stores categorized tickets into Google BigQuery (`triage_tickets` table) in real-time.
* **Live Command Center:** A dynamic, responsive dashboard tracking total ticket volume and critical risk alerts.
* **Frictionless UI:** Features a mobile-responsive interface with a seamless Dark/Light mode theme to ensure accessibility.

## 🏗️ System Architecture
The application follows a robust, enterprise-grade cloud architecture:
1. **Frontend:** HTML5 / CSS3 / Vanilla JavaScript (Responsive Dark/Light UI)
2. **Backend Engine:** Java / Spring Boot 3 (REST API routing)
3. **AI Intelligence Layer:** Google Cloud Vertex AI (Gemini 2.5 Flash)
4. **Data Warehouse:** Google BigQuery (`civic_data` dataset)

### Workflow
`Citizen UI` ➔ `Spring Boot API` ➔ `Vertex AI (Analysis)` ➔ `BigQuery (Storage)` ➔ `City Dashboard (Live Sync)`

## 💻 Local Setup & Installation

**Prerequisites:**
* Java 17 or higher
* Maven
* Google Cloud CLI (`gcloud`) installed and configured

**Steps:**
1. Clone the repository:
   ```bash
   git clone [https://github.com/varshajainu/civic-core.git](https://github.com/varshajainu/civic-core.git)
   cd civic-core