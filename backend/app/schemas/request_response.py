from pydantic import BaseModel
from typing import Optional, List, Literal

class Telemetry(BaseModel):
    speed_mps: Optional[float] = 0.0
    location_type: Optional[str] = "UNKNOWN" # INDOOR / OUTDOOR

class AnalysisRequest(BaseModel):
    image_base64: str
    user_intent: Literal["AUTO", "NAVIGATION", "READING", "GENERAL"] = "AUTO"
    telemetry: Optional[Telemetry] = None
    audio_query: Optional[str] = None

class Action(BaseModel):
    type: Literal["TTS", "HAPTIC"]
    content: str # Speech text or Haptic pattern name

class AnalysisResponse(BaseModel):
    agent_used: str
    actions: List[Action]
    suggested_mode_switch: Optional[str] = None
