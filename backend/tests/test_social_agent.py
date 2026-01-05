import pytest
from unittest.mock import AsyncMock, patch
from app.schemas.request_response import AnalysisRequest
from app.agents.perception.social import social_agent

@pytest.mark.asyncio
async def test_social_agent_workflow():
    # 1. Setup Request
    request = AnalysisRequest(
        image_base64="dummy_face_image",
        user_intent="GENERAL",
        user_id="user_123"
    )
    
    # 2. Mock Logic
    fake_llm_json = """
    ```json
    {
        "people": [
            { "name": "Alice", "confidence": "High", "emotion": "Joyful", "description": "Smiling user" }
        ],
        "summary": "Alice is smiling at you."
    }
    ```
    """
    
    fake_memories = [
        {"content": "Alice is my sister with red hair.", "id": "mem_1"}
    ]
    
    with patch("app.services.llm_gateway.llm_gateway.generate_response", new_callable=AsyncMock) as mock_generate, \
         patch("app.services.memory.memory_service.search_memories", new_callable=AsyncMock) as mock_search:
         
        mock_generate.return_value = fake_llm_json
        mock_search.return_value = fake_memories
        
        # 3. Execution
        response = await social_agent.analyze(request)
        
        # 4. Assertions
        assert response.agent_used == "SocialAgent"
        tts_action = response.actions[0]
        assert tts_action.type == "TTS"
        assert "Alice" in tts_action.content
        assert "smiling" in tts_action.content
        
        # Verify Prompt included memories
        prompt_args = mock_generate.call_args[1]["system_prompt"]
        assert "Alice is my sister" in prompt_args
