import pytest
from unittest.mock import AsyncMock, patch, MagicMock
from app.services.memory import MemoryService

@pytest.fixture
def mock_supabase():
    with patch("app.services.memory.create_client") as mock_create:
        mock_client = MagicMock()
        mock_create.return_value = mock_client
        yield mock_client

@pytest.fixture
def mock_llm_gateway():
    with patch("app.services.llm_gateway.llm_gateway") as mock_gw:
        mock_gw.get_embedding = AsyncMock(return_value=[0.1, 0.2, 0.3])
        yield mock_gw

@pytest.mark.asyncio
async def test_store_memory_success(mock_supabase, mock_llm_gateway):
    # Setup
    service = MemoryService()
    service.client = mock_supabase # Force inject mock
    
    # Execute
    await service.store_memory("user123", "Keys are on table")
    
    # Verify Embedding called
    mock_llm_gateway.get_embedding.assert_awaited_with("Keys are on table")
    
    # Verify DB Insert called
    mock_supabase.table.assert_called_with("memories")
    mock_supabase.table().insert.assert_called()
    mock_supabase.table().insert().execute.assert_called()

@pytest.mark.asyncio
async def test_search_memories_success(mock_supabase, mock_llm_gateway):
    # Setup
    service = MemoryService()
    service.client = mock_supabase
    
    # Mock RPC response
    mock_response = MagicMock()
    mock_response.data = [{"content": "Memory 1"}, {"content": "Memory 2"}]
    mock_supabase.rpc.return_value.execute.return_value = mock_response
    
    # Execute
    results = await service.search_memories("user123", "Where are keys?")
    
    # Verify
    assert len(results) == 2
    assert results[0] == "Memory 1"
    mock_supabase.rpc.assert_called_with("match_memories", {
        "query_embedding": [0.1, 0.2, 0.3],
        "match_threshold": 0.7,
        "match_count": 3
    })
