import pytest
from unittest.mock import AsyncMock, patch
from app.agents.perception.awareness import awareness_agent
from app.schemas.request_response import AnalysisRequest, Telemetry

@pytest.mark.asyncio
async def test_awareness_includes_compass_data():
    # Setup Request with Telemetry
    request = AnalysisRequest(
        image_base64="dummy",
        telemetry=Telemetry(heading=180.0, pitch=45.0) # User facing South
    )
    
    mock_json = """
    ```json 
    { "summary": "Compass data received." } 
    ```
    """
    
    with patch("app.services.llm_gateway.llm_gateway.generate_response", new_callable=AsyncMock) as mock_generate, \
         patch("app.services.memory.memory_service.search_memories", new_callable=AsyncMock) as mock_search, \
         patch("app.services.memory.memory_service.store_memory", new_callable=AsyncMock) as mock_store:
         
        mock_generate.return_value = mock_json
        mock_search.return_value = [] # No memories
        
        # Act
        await awareness_agent.analyze(request)
        
        # Assert
        mock_generate.assert_called_once()
        args, kwargs = mock_generate.call_args
        prompt = kwargs["system_prompt"]
        
        # Verify Compass Heading is in the prompt
        assert "Heading=180.0 deg" in prompt
        assert "Pitch=45.0 deg" in prompt
