from app.schemas.request_response import AnalysisRequest, AnalysisResponse, Action
from app.agents.base import BaseAgent
from app.agents.navigation import navigation_agent
from app.agents.reading import reading_agent

class OrchestratorService:
    """
    The Brain of the operation.
    Decides which agent to invoke based on User Intent and Telemetry.
    Uses Dependency Injection for Agent instances.
    """
    
    def __init__(self, 
                 navigation_agent_instance: BaseAgent = navigation_agent,
                 reading_agent_instance: BaseAgent = reading_agent):
        
        self.navigation_agent = navigation_agent_instance
        self.reading_agent = reading_agent_instance

    async def process_request(self, request: AnalysisRequest) -> AnalysisResponse:
        print(f"[Orchestrator] Processing Intent: {request.user_intent}")
        
        if request.user_intent == "NAVIGATION":
            return await self._route_to_navigation(request)
        elif request.user_intent == "READING":
            return await self._route_to_reading(request)
            
        if request.telemetry and request.telemetry.speed_mps > 1.0:
            print("[Orchestrator] High speed detected. Auto-routing to Navigation.")
            return await self._route_to_navigation(request)
            
        return await self._route_to_describer(request)

    async def _route_to_navigation(self, request: AnalysisRequest) -> AnalysisResponse:
        return await self.navigation_agent.analyze(request.image_base64)

    async def _route_to_reading(self, request: AnalysisRequest) -> AnalysisResponse:
        return await self.reading_agent.analyze(request.image_base64)

    async def _route_to_describer(self, request: AnalysisRequest) -> AnalysisResponse:
        return AnalysisResponse(
            agent_used="DescriberAgent",
            actions=[Action(type="TTS", content="I see a room with... (General Description)")]
        )

# Singleton Instance
orchestrator = OrchestratorService()
