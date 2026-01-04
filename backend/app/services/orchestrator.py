from app.schemas.request_response import AnalysisRequest, AnalysisResponse, Action
from app.agents.core.base import BaseAgent
from app.agents.core.guardian import guardian_agent
from app.agents.navigation.indoor import indoor_navigation_agent
from app.agents.navigation.outdoor import outdoor_navigation_agent
from app.agents.perception.reading import reading_agent
from app.agents.perception.awareness import awareness_agent

class OrchestratorService:
    """
    The Brain of the operation.
    Decides which agent to invoke based on User Intent and Telemetry.
    Uses Dependency Injection for Agent instances.
    """
    
    def __init__(self, 
                 guardian_agent_instance: BaseAgent = guardian_agent,
                 indoor_agent_instance: BaseAgent = indoor_navigation_agent,
                 outdoor_agent_instance: BaseAgent = outdoor_navigation_agent,
                 reading_agent_instance: BaseAgent = reading_agent,
                 describer_agent_instance: BaseAgent = describer_agent,
                 finder_agent_instance: BaseAgent = object_finder_agent,
                 awareness_agent_instance: BaseAgent = awareness_agent):
        
        self.guardian = guardian_agent_instance
        self.indoor_agent = indoor_agent_instance
        self.outdoor_agent = outdoor_agent_instance
        self.reading_agent = reading_agent_instance
        self.describer_agent = describer_agent_instance
        self.finder_agent = finder_agent_instance
        self.awareness_agent = awareness_agent_instance
        self.history = [] # Phase 5.2 Memory




    async def process_request(self, request: AnalysisRequest) -> AnalysisResponse:
        print(f"[Orchestrator] Processing Intent: {request.user_intent}")
        
        # -1. Guardian Check (Safety Layer) - ALWAYS RUNS
        danger_response = await self.guardian.analyze(request)
        if danger_response:
             print("[Orchestrator] GUARDIAN INTERVENTION! Danger Detected.")
             return danger_response

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
            if "screen reader" in q and ("enable" in q or "on" in q or "start" in q):
                 return AnalysisResponse(
                    agent_used="Orchestrator",
                    actions=[
                        Action(type="SETTING_UPDATE", content="IS_TTS_ENABLED=FALSE"),
                        Action(type="TTS", content="Turning off app voice. Screen reader mode enabled.")
                    ]
                )
            if "screen reader" in q and ("disable" in q or "off" in q or "stop" in q):
                 return AnalysisResponse(
                    agent_used="Orchestrator",
                    actions=[
                        Action(type="SETTING_UPDATE", content="IS_TTS_ENABLED=TRUE"),
                        Action(type="TTS", content="Turning on app voice. Screen reader mode disabled.")
                    ]
                )
            
            # Check for generic TTS toggle
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

            # Check for Search Intent (Implicit)
            if any(term in q for term in ["find", "where", "locate", "search", "looking for"]):
                 print(f"[Orchestrator] Search query detected: '{q}'. Routing to Finder.")
                 return await self._route_to_finder(request)
        
        # 1. Direct Intent Routing
        if request.user_intent == "NAVIGATION":
            return await self._route_to_navigation(request)
        elif request.user_intent == "READING":
            return await self._route_to_reading(request)
        elif request.user_intent == "SEARCH":
            return await self._route_to_finder(request)
            
        # 2. Auto-Routing (Context)
        # If speed > 1.0 m/s -> Navigation
        if request.telemetry and request.telemetry.speed_mps > 1.0:
            print("[Orchestrator] High speed detected. Auto-routing to Navigation.")
            return await self._route_to_navigation(request)

        # If Looking For is set, but intent wasn't explicitly SEARCH -> Finder
        if request.looking_for:
             return await self._route_to_finder(request)
            
        # 3. Default Fallback
        return await self._route_to_describer(request)

    async def _route_to_navigation(self, request: AnalysisRequest) -> AnalysisResponse:
        # Smart Switching based on Telemetry
        if request.telemetry and request.telemetry.location_type == "OUTDOOR":
            print("[Orchestrator] Routing to OUTDOOR Agent.")
            return await self.outdoor_agent.analyze(request)
        else:
            # Default to Indoor for safety if Unknown or explicitly Indoor
            print("[Orchestrator] Routing to INDOOR Agent.")
            return await self.indoor_agent.analyze(request)

    async def _route_to_reading(self, request: AnalysisRequest) -> AnalysisResponse:
        return await self.reading_agent.analyze(request)

    async def _route_to_describer(self, request: AnalysisRequest) -> AnalysisResponse:
        # UPGRADE: Phase 5 - Use Awareness Agent instead of basic Describer
        # UPGRADE: Phase 5 - Use Awareness Agent with Memory
        print("[Orchestrator] Routing to AWARENESS Agent (Phase 5).")
        response = await self.awareness_agent.analyze(request, history=self.history)
        
        # Update History
        query = request.audio_query or "Describe scene"
        if response and response.actions and response.actions[0].type == "TTS":
             self.history.append({"query": query, "response": response.actions[0].content})
             
        return response

    async def _route_to_finder(self, request: AnalysisRequest) -> AnalysisResponse:
        return await self.finder_agent.analyze(request)

# Singleton Instance
orchestrator = OrchestratorService()
