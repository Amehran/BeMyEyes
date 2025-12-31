from app.schemas.request_response import AnalysisRequest, AnalysisResponse, Action
from app.agents.base import BaseAgent
from app.agents.navigation import navigation_agent
from app.agents.reading import reading_agent
from app.agents.describer import describer_agent

class OrchestratorService:
    """
    The Brain of the operation.
    Decides which agent to invoke based on User Intent and Telemetry.
    Uses Dependency Injection for Agent instances.
    """
    
    def __init__(self, 
                 navigation_agent_instance: BaseAgent = navigation_agent,
                 reading_agent_instance: BaseAgent = reading_agent,
                 describer_agent_instance: BaseAgent = describer_agent):
        
        self.navigation_agent = navigation_agent_instance
        self.reading_agent = reading_agent_instance
        self.describer_agent = describer_agent_instance

    async def process_request(self, request: AnalysisRequest) -> AnalysisResponse:
        print(f"[Orchestrator] Processing Intent: {request.user_intent}")
        
        # 0. Voice Command Interception
        if request.audio_query:
            q = request.audio_query.lower()
            print(f"[Orchestrator] Voice Query Received: '{q}'")
            
            trigger_words = ["switch", "mode", "enable", "set", "turn", "change"]
            
            # Check for Outdoor
            if any(term in q for term in ["outdoor", "out door", "outside", "outdo"]) and any(word in q for word in trigger_words):
                return AnalysisResponse(
                    agent_used="Orchestrator",
                    actions=[
                        Action(type="SETTING_UPDATE", content="OUTDOOR=TRUE"),
                        Action(type="TTS", content="I have enabled Outdoor Mode. I will now look for paths and obstacles.")
                    ]
                )
            # Check for Indoor
            if any(term in q for term in ["indoor", "in door", "inside"]) and any(word in q for word in trigger_words):
                return AnalysisResponse(
                    agent_used="Orchestrator",
                    actions=[
                        Action(type="SETTING_UPDATE", content="OUTDOOR=FALSE"),
                        Action(type="TTS", content="I have enabled Indoor Mode. I will focus on objects and details.")
                    ]
                )

            # Check for TTS / Screen Reader
            # Enable Screen Reader -> App Silent (TTS OFF)
            if "screen reader" in q and ("enable" in q or "on" in q or "start" in q):
                 return AnalysisResponse(
                    agent_used="Orchestrator",
                    actions=[
                        Action(type="SETTING_UPDATE", content="IS_TTS_ENABLED=FALSE"),
                        Action(type="TTS", content="Turning off app voice. Screen reader mode enabled.")
                    ]
                )
            # Disable Screen Reader -> App Voice ON (TTS ON)
            if "screen reader" in q and ("disable" in q or "off" in q or "stop" in q):
                 return AnalysisResponse(
                    agent_used="Orchestrator",
                    actions=[
                        Action(type="SETTING_UPDATE", content="IS_TTS_ENABLED=TRUE"),
                        Action(type="TTS", content="Turning on app voice. Screen reader mode disabled.")
                    ]
                )

            if any(term in q for term in ["tts", "voice feedback", "speech"]) and any(word in q for word in ["off", "disable", "stop"]):
                 return AnalysisResponse(
                    agent_used="Orchestrator",
                    actions=[
                        Action(type="SETTING_UPDATE", content="IS_TTS_ENABLED=FALSE"),
                        Action(type="TTS", content="Turning off voice feedback.")
                    ]
                )
            if any(term in q for term in ["tts", "voice feedback", "speech"]) and any(word in q for word in ["on", "enable", "start"]):
                 return AnalysisResponse(
                    agent_used="Orchestrator",
                    actions=[
                        Action(type="SETTING_UPDATE", content="IS_TTS_ENABLED=TRUE"),
                        Action(type="TTS", content="Voice feedback enabled.")
                    ]
                )
            
            # Check for Realtime Detection
            if any(term in q for term in ["realtime", "detection", "detecting"]) and any(word in q for word in ["off", "disable", "stop"]):
                 return AnalysisResponse(
                    agent_used="Orchestrator",
                    actions=[
                        Action(type="SETTING_UPDATE", content="REALTIME_ENABLED=FALSE"),
                        Action(type="TTS", content="Realtime object detection stopped.")
                    ]
                )
            if any(term in q for term in ["realtime", "detection", "detecting"]) and any(word in q for word in ["on", "enable", "start"]):
                 return AnalysisResponse(
                    agent_used="Orchestrator",
                    actions=[
                        Action(type="SETTING_UPDATE", content="REALTIME_ENABLED=TRUE"),
                        Action(type="TTS", content="Realtime object detection started.")
                    ]
                )
        
        # 1. Direct Intent Routing
        if request.user_intent == "NAVIGATION":
            return await self._route_to_navigation(request)
        elif request.user_intent == "READING":
            return await self._route_to_reading(request)
            
        # 2. Auto-Routing (Context)
        if request.telemetry and request.telemetry.speed_mps > 1.0:
            print("[Orchestrator] High speed detected. Auto-routing to Navigation.")
            return await self._route_to_navigation(request)
            
        # 3. Default Fallback
        return await self._route_to_describer(request)

    async def _route_to_navigation(self, request: AnalysisRequest) -> AnalysisResponse:
        return await self.navigation_agent.analyze(request)

    async def _route_to_reading(self, request: AnalysisRequest) -> AnalysisResponse:
        return await self.reading_agent.analyze(request)

    async def _route_to_describer(self, request: AnalysisRequest) -> AnalysisResponse:
        return await self.describer_agent.analyze(request)

# Singleton Instance
orchestrator = OrchestratorService()
