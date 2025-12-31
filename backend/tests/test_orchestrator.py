import pytest
from unittest.mock import AsyncMock, patch
from app.services.orchestrator import OrchestratorService
from app.schemas.request_response import AnalysisRequest, Telemetry

@pytest.mark.asyncio
async def test_orchestrator_routes_to_navigation_on_high_speed():
    # 1. Arrange: Create a Mock Agent
    mock_agent = AsyncMock()
    mock_agent.analyze.return_value = "Success" # Simplification
    
    # Inject Mock Agent into Orchestrator
    orchestrator = OrchestratorService(navigation_agent_instance=mock_agent)
    
    request = AnalysisRequest(
        image_base64="dummy",
        user_intent="AUTO",
        telemetry=Telemetry(speed_mps=5.0, location_type="OUTDOOR")
    )
    
    # 2. Act
    await orchestrator.process_request(request)
    
    # 3. Assert: Verify the mock was called
    mock_agent.analyze.assert_called_once_with("dummy")

@pytest.mark.asyncio
async def test_orchestrator_routes_explicit_intent():
    # 1. Arrange
    mock_agent = AsyncMock()
    orchestrator = OrchestratorService(navigation_agent_instance=mock_agent)
    
    request = AnalysisRequest(image_base64="dummy", user_intent="NAVIGATION", telemetry=None)
    
    # 2. Act
    await orchestrator.process_request(request)
    
    # 3. Assert
    mock_agent.analyze.assert_called_once()
