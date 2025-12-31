from abc import ABC, abstractmethod
from app.schemas.request_response import AnalysisResponse, AnalysisRequest

class BaseAgent(ABC):
    """
    Abstract Base Class for all Agents.
    Enforces a consistent interface for the Orchestrator to use.
    """
    
    @abstractmethod
    async def analyze(self, request: AnalysisRequest) -> AnalysisResponse:
        """
        Main entry point for the agent.
        Must return a structured AnalysisResponse.
        """
        pass
