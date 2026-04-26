# End Route Logistics (ERL)
### *AI-Powered Freight Intelligence & Multi-Modal Route Optimization*

**End Route Logistics (ERL)** is a cloud-native logistics engine designed to navigate the complexities of modern supply chains. Developed for the **Google Solution Challenge 2026**, ERL leverages **Gemini 3 Flash** and real-time environmental data to provide intelligent, ranked carrier recommendations. 

---

## Key Features
* **AI-Driven Reasoning:** Uses Gemini 3 Flash to evaluate trade-offs between cost, speed, and safety.
* **Weather-Aware Routing:** Integrated with OpenWeather API to detect potential delays.
* **Multi-Modal Analysis:** Ranks carriers across Air, Rail, Road, and Sea.
* **Carbon Footprint Optimization:** Prioritizes high-efficiency transit modes.

---

## Technical Stack

| Category | Technology |
| :--- | :--- |
| **Frontend** | HTML5, CSS3, JavaScript (ES6+), Hosted on **Vercel** |
| **Backend** | Java 21, Spring Boot 3.4, Maven, Hosted on **Render** |
| **AI Intelligence** | Google Gemini 3.1 Flash lite preview (Google AI Studio) |
| **Infrastructure** | Docker, Cron-Job.org (Uptime Monitoring) |

---

## Architecture & Logic
ERL utilizes a **Contextual Analysis Engine** to score carriers. The backend fetches live weather, combines it with carrier metadata, and passes it to the LLM for reasoning.

$$EfficiencyScore = \frac{Speed(km/h) \times Reliability(\%)}{Cost(₹/kg) \times RiskScore}$$

---

## UN Sustainable Development Goal Alignment
* **SDG 9:** Industry, Innovation, and Infrastructure.
* **SDG 12:** Responsible Consumption and Production.

---

## Installation
```bash
git clone [https://github.com/harshitsaini-hub/EndRouteLogistics.git](https://github.com/harshitsaini-hub/EndRouteLogistics.git)
mvn clean package
java -jar target/EndRouteLogistics-0.0.1-SNAPSHOT.jar
