## End Route Logistics (ERL)
AI-Powered Freight Intelligence & Multi-Modal Route Optimization

End Route Logistics (ERL) is a cloud-native logistics engine built to simplify and optimize modern supply chains. Developed for the Google Solution Challenge 2026, ERL leverages Gemini 3 Flash and real-time environmental data to deliver intelligent, ranked carrier recommendations.

## Key Features
AI-Driven Decision Engine
Uses Gemini 3 Flash to evaluate trade-offs between cost, speed, and reliability.
Weather-Aware Routing
Integrates with OpenWeather API to proactively identify potential delays.
Multi-Modal Optimization
Compares and ranks carriers across Air, Rail, Road, and Sea.
Carbon Efficiency Awareness
Encourages environmentally efficient logistics decisions.
 Architecture & Core Logic

ERL uses a Contextual Analysis Engine that combines:

Carrier metadata
Environmental conditions (weather)
AI-driven reasoning

to generate ranked logistics recommendations.

## Scoring Formula:

EfficiencyScore = (Speed × Reliability) / (Cost × RiskScore)
## Tech Stack
Layer	Technology
Frontend	HTML5, CSS3, JavaScript (ES6+), deployed on Vercel
Backend	Java 21, Spring Boot 3.4, Maven, deployed on Render
AI Integration	Google Gemini 3.1 Flash (Google AI Studio)
Infrastructure	Docker, Cron-Job.org (uptime monitoring)
 UN SDG Alignment
SDG 9 — Industry, Innovation, and Infrastructure
SDG 12 — Responsible Consumption and Production
 Team & Contributions

This project was developed as part of the Google Solution Challenge 2026.

## Team & Contributions

This project was developed as part of the Google Solution Challenge 2026.

Harshit Saini
Backend Engineering, AI Integration, System Architecture, Deployment
Aditya Paswan
Frontend Development and UI Design
Mayank Sharma
Research support, requirement discussions, and project coordination
Parv Modi
Ideation support and final-phase review

This project reflects a collaborative effort, combining system design, user experience, and problem-solving to build an AI-powered logistics solution.

## Installation
git clone https://github.com/harshitsaini-hub/EndRouteLogistics.git
cd EndRouteLogistics
mvn clean package
java -jar target/EndRouteLogistics-0.0.1-SNAPSHOT.jar
## Future Improvements
Real-time traffic integration
Advanced cost prediction using ML models
Carrier API integrations for live pricing
User dashboard & analytics

## License

This project is developed for educational and competition purposes.
