# Live Chess Ratings

A real-time chess rating tracking system that monitors live broadcasts from Lichess, processes games, and maintains up-to-date player ratings.

## Overview

This application scrapes live chess broadcasts from Lichess, processes games in real-time, calculates rating changes using ELO calculations, and provides cached top 300 player ratings for different time controls (Standard, Rapid, Blitz).

## Features

- **Live Broadcast Discovery**: Automatically discovers ongoing and recently finished broadcasts from Lichess API
- **Real-time Game Processing**: Processes PGN games to extract player data, moves, and results
- **Rating Calculations**: Computes Elo rating changes based on game outcomes
- **Multi-Format Support**: Tracks Standard, Rapid, and Blitz ratings separately
- **Intelligent Caching**: In-memory cache for top 300 players updated every 5 minutes
- **Automatic Cleanup**: Monitors ongoing rounds and marks them as finished when appropriate
- **Batch Import**: Imports and updates player ratings from FIDE database

## Tech Stack

- **Backend Framework**: Spring Boot 4.0.5
- **Language**: Java 25
- **Database**: PostgreSQL 17
- **Build Tool**: Maven
- **API Client**: WebClient (Spring WebFlux)
- **Caching**: Caffeine
- **Containerization**: Docker & Docker Compose

## Project Structure

```
live-chess-ratings/
├── src/main/java/com/example/demo/
│   ├── DemoApplication.java              # Main Spring Boot application
│   ├── LiveRatingInit.java               # Initialization logic
│   ├── batch/                            # Batch jobs for data import
│   │   ├── FideImportJob.java           # FIDE player import job
│   │   └── ChangeActivenessJob.java     # Player activeness update job
│   ├── broadcast/                        # Lichess broadcast integration
│   │   └── BroadcastDiscoveryWorker.java # Broadcast discovery & game fetching
│   ├── config/                           # Configuration classes
│   │   ├── CacheConfiguration.java      # Caffeine cache setup
│   │   └── CorsConfiguration.java       # CORS settings
│   ├── controller/                       # REST endpoints
│   ├── dto/                              # Data transfer objects
│   ├── entity/                           # JPA entities
│   ├── repository/                       # Data access layer
│   ├── service/                          # Business logic
│   │   ├── GameProcessingService.java   # Game PGN processing
│   │   ├── LiveRatingRefreshService.java # Rating calculations
│   │   └── LiveRatingCacheService.java  # Cache management
│   └── utils/                            # Utility classes
├── src/main/resources/
│   ├── application.properties            # Spring configuration
│   ├── logback-spring.xml                # Logging configuration
│   └── db/migration/                     # Flyway DB migrations
├── compose.yaml                          # Docker Compose configuration
└── pom.xml                               # Maven dependencies
```

## Getting Started

### Prerequisites

- Java 25 or higher
- Docker & Docker Compose
- Maven (or use `./mvnw` wrapper)

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd live-chess-ratings
   ```

2. **Start the database**
   ```bash
   docker-compose up -d
   ```

3. **Build the project**
   ```bash
   mvn clean package
   ```

4. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

The application will start on `http://localhost:8080`

## Configuration

### Database Setup

The database is configured in `application.properties`:
- **Host**: localhost:5433
- **Username**: peaceful
- **Password**: secret
- **Database**: chess

### Environment Variables

Set these in your `.env` file or environment:

```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/chess
SPRING_DATASOURCE_USERNAME=peaceful
SPRING_DATASOURCE_PASSWORD=secret
```

## API Endpoints

### Ratings Endpoints

- **GET** `/top-ratings` - All top 300 ratings (Standard, Rapid, Blitz)
- **GET** `/std-ratings` - Top 300 Standard ratings
- **GET** `/rapid-ratings` - Top 300 Rapid ratings
- **GET** `/blitz-ratings` - Top 300 Blitz ratings

### Monthly Ratings Endpoints

- **POST** `/monthly-ratings/import` - Import monthly ratings from FIDE
  ```json
  {
    "fileUrl": "https://example.com/players.xml",
    "importDate": "2026-05-01"
  }
  ```

- **POST** `/monthly-ratings/activeness` - Update player activeness status
  ```json
  {
    "fileUrl": "https://example.com/players.xml",
    "timeControl": "STD"
  }
  ```

## Scheduled Tasks

### Broadcast Discovery (Every 5 minutes)
- Fetches live broadcasts from Lichess API
- Processes game data
- Updates player ratings in real-time

### Cache Refresh (Every 5 minutes)
- Updates in-memory cache with top 300 players
- Aggregates recent game history per player
- Calculates rating changes

### Ongoing Rounds Cleanup (Every 10 minutes)
- Checks status of ongoing broadcast rounds
- Marks finished rounds appropriately
- Maintains accurate round status

## Data Models

### Player
- FIDE ID (unique identifier)
- Name, Country, Birthday
- Rating K-factors for different time controls
- Activeness flags

### Game
- Game ID, Result, Date
- White/Black player IDs and ratings
- Rating changes per player
- Move count and last move
- Time control classification

### Live Rating
- Current ratings (Standard, Rapid, Blitz)
- Rating changes
- Associated player

### Tournament & Broadcast Round
- Tournament metadata
- Round status (UNSTARTED, ONGOING, FINISHED)
- Start/end timestamps

## Performance Optimization

### Caching Strategy
- Top 300 players cached in-memory for 5 minutes
- Separate caches for each time control
- Reduces database queries significantly

### Bandwidth Optimization
- Uses Lichess API's `live=true` parameter
- Only fetches ongoing/recently finished broadcasts
- Filters out unrated tournaments

### Database Optimization
- Flyway migrations for schema management
- Indexed queries for top players
- Prepared statements for game lookups

## Frontend

The frontend application is available at: **https://github.com/ShohjahonAhmad/live-chess-ratings-frontend**

The frontend provides:
- Real-time rating leaderboards
- Player search and detailed profiles
- Game history and statistics
- Rating trend charts

## Logging

Logs are written to:
- **Console**: Real-time application events
- **File**: `logs/app.*.gz` (rotated daily)
- **Configuration**: `src/main/resources/logback-spring.xml`

## Building & Deployment

### Build JAR
```bash
mvn clean package -DskipTests
```

### Build Docker Image
```bash
docker build -t live-chess-ratings:latest .
```

### Docker Compose Deployment
```bash
docker-compose -f compose.yaml up -d
```

## Testing

Run tests with:
```bash
mvn test
```

## Troubleshooting

### Database Connection Issues
- Ensure PostgreSQL is running: `docker-compose ps`
- Check database credentials in `application.properties`
- Verify port 5433 is not blocked

### Broadcast Discovery Not Working
- Check Lichess API availability
- Review application logs: `tail -f logs/app.log`
- Verify internet connectivity

### Cache Not Updating
- Check cache refresh scheduled task logs
- Verify database has at least 300 rated players
- Ensure players have associated games

## Contributing

To contribute to this project:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## License

MIT License

## Support

For issues, questions, or suggestions:
- Open an issue on GitHub
- Check existing documentation
- Review application logs

---

**Last Updated**: May 2026
**Version**: 1.0.0

