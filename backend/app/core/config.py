from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    PROJECT_NAME: str = "BeMyEyes Backend"
    API_V1_STR: str = "/api/v1"
    GEMINI_API_KEY: str = "" # Provide via .env file or environment variable

    class Config:
        env_file = ".env"

settings = Settings()
