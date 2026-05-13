# News MCP Server (Kotlin)

A Model Context Protocol (MCP) server that provides news tools using the Kotlin SDK.

## Features

- **get_top_news**: Fetch top headlines from various categories (Business, Entertainment, Health, Science, Sports, Technology, World).
- **search_news**: Search for news articles by keyword using Google News RSS.
- **get_news**: Get news articles for specific topics within a certain time range (hours).
- **get_news_detail**: Fetch and parse the full text content of a specific news article by its URL.

## Prerequisites

- Java 17 or higher

## Building

### Create a Single Executable Binary (Shadow JAR)

The easiest way to distribute this server is to create a "Fat JAR" which contains all dependencies in a single file.

```bash
gradle shadowJar
```

The resulting binary will be located at:
`build/libs/news-mcp-1.0.0-all.jar`

## Usage

### Running the Binary Directly

You can run the server on any machine with Java 17+ installed without needing the source code:

```bash
java -jar build/libs/news-mcp-1.0.0-all.jar
```

### Using from Jupyter Notebook

You can consume this MCP server in a Jupyter Notebook. Since it's a single JAR, it's very easy to reference.

1.  **Install the MCP Python SDK**:
    ```bash
    pip install mcp
    ```

2.  **Use the following Python code to call the server**:

```python
import asyncio
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client

async def run_news_mcp():
    # Configure the server parameters
    # Replace the path with the absolute path to your news-mcp-1.0.0-all.jar
    server_params = StdioServerParameters(
        command="java",
        args=["-jar", "/path/to/news-mcp-1.0.0-all.jar"],
        env=None
    )

    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            # Initialize the session
            await session.initialize()

            # List available tools
            tools = await session.list_tools()
            print("Available Tools:", [tool.name for tool in tools.tools])

            # Call the 'get_top_news' tool
            print("\nFetching Technology news...")
            result = await session.call_tool("get_top_news", arguments={"category": "Technology"})
            
            for content in result.content:
                print(content.text)

            # Call the 'get_news' tool with topics
            print("\nSearching for specific topics...")
            search_result = await session.call_tool("get_news", arguments={
                "topics": ["Kotlin", "Artificial Intelligence"],
                "hours": 12
            })
            
            for content in search_result.content:
                print(content.text)

# Run the async function
await run_news_mcp()
```

## Configuration for Claude Desktop

Add the following to your `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "news": {
      "command": "java",
      "args": ["-jar", "/absolute/path/to/news-mcp-1.0.0-all.jar"]
    }
  }
}
```

## Tools

### `get_top_news`
Arguments:
- `category` (optional): One of `Business`, `Entertainment`, `Health`, `Science`, `Sports`, `Technology`, `World`. Default is `World`.

### `search_news`
Arguments:
- `query` (required): The search keyword or phrase.

### `get_news`
Arguments:
- `topics` (required): A list of strings representing the topics to search for.
- `hours` (optional): The time range in hours (e.g., 24 for the last day). Default is 24.

### `get_news_detail`
Arguments:
- `url` (required): The direct URL of the news article to fetch.
