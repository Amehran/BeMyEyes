from pydantic import BaseModel
from typing import Optional, List, Literal

class Telemetry(BaseModel):
    speed_mps: Optional[float] = 0.0
    location_type: Optional[str] = "UNKNOWN" # INDOOR / OUTDOOR
    heading: Optional[float] = 0.0 # 0-360 degrees (North=0)
    pitch: Optional[float] = 0.0 # -90 (Down) to +90 (Up)

class AnalysisRequest(BaseModel):
    image_base64: str
    user_intent: Literal["AUTO", "NAVIGATION", "READING", "GENERAL", "SEARCH"] = "AUTO"
    telemetry: Optional[Telemetry] = None
    audio_query: Optional[str] = None
    language: Optional[str] = "en"
    looking_for: Optional[str] = None # For ObjectFinder Agent ("keys", "exit", etc.)
    user_id: Optional[str] = "default_user" # For Memory/Persistence

class Action(BaseModel):
    type: Literal["TTS", "HAPTIC", "SETTING_UPDATE"]
    content: str # Speech text or Haptic pattern name

class AnalysisResponse(BaseModel):
    agent_used: str
    actions: List[Action]
    suggested_mode_switch: Optional[str] = None
