# PinterQ - Core API & AI Engine ⚙️🤖

This repository houses the backend architecture for PinterQ. Built with Java Spring Boot, it serves as the central brain of the application—handling relational data persistence, business logic, and orchestrating prompts with Google's Gemini AI to generate adaptive educational content.

## ✨ Features
* **Generative AI Integration:** Communicates with the `gemini-1.5-flash` model via REST to parse raw notes into structured JSON quizzes and flashcards.
* **Adaptive Prompt Engineering:** Dynamically injects targeted context (HOTS vs. Basic Concepts) based on frontend triggers to adjust quiz difficulty.
* **Relational Data Management:** Manages hierarchical data mappings between Users, Categories (Subjects), Materials, Flashcards, and Quizzes.
* **Robust REST API:** Exposes clean endpoints for seamless frontend consumption.

## 🛠️ Tech Stack
* **Language:** Java 21
* **Framework:** Spring Boot 3.x
* **Database:** PostgreSQL
* **ORM:** Spring Data JPA / Hibernate
* **AI Provider:** Google Gemini API

## 🚀 Getting Started

### Prerequisites
* Java Development Kit (JDK) 21
* PostgreSQL running locally or on a server.
* A valid Google Gemini API Key.

### Database Setup
Create a PostgreSQL database named `pinterq_db`. The Spring Boot application (via Hibernate) will automatically generate the required tables upon startup.

### Configuration
1. Clone the repository:
   ```bash
   git clone [https://github.com/aucloire/II2210-18224071-PinterQ-BackEnd](https://github.com/aucloire/II2210-18224071-PinterQ-BackEnd)
2. Navigate to the `src/main/resources/application.properties` file and configure your database credentials:
   ``` bash
   spring.datasource.url=jdbc:postgresql://localhost:5432/pinterq_db
   spring.datasource.username=YOUR_DB_USERNAME
   spring.datasource.password=YOUR_DB_PASSWORD
3. **Important:** Ensure your Gemini API Key is configured in GeminiAiService.java or via environment variables.

### Running the Application
Use the Gradle wrapper to build and run the application:
   ``` bash
   ./gradlew bootRun
   ```
The server will start on `http://localhost:8080`.

## 📡 Core API Endpoint
- `POST /api/auth/register` & `/login` - User authentication.
- `POST /api/categories` - Create new study subjects.
- `POST /api/study/generate` - Generate base flashcards and quizzes from text.
- `POST /api/study/generate-adaptive` - Generate difficulty-adjusted quizzes based on user performance.