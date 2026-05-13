# News MCP Server (Kotlin)

A Model Context Protocol (MCP) server that provides news tools using the Kotlin SDK.

## Features

- **get_top_news**: Fetch top headlines from various categories (Business, Entertainment, Health, Science, Sports, Technology, World).
- **search_news**: Search for news articles by keyword using Google News RSS.
- **get_news**: Get news articles for specific topics within a certain time range (hours).
- **get_news_detail**: Fetch and parse the full text content of a specific news article by its URL.

## Prerequisites

- Java 21 or higher
- Gradle (included via wrapper if you generate one, or use system gradle)

## Building

Build the project using Gradle:

```bash
gradle build
```

The executable JAR will be located at `build/libs/news-mcp-all.jar` (if you use a shadow jar) or you can run it via Gradle.
To create a fat JAR, you might want to add the shadow plugin, but you can also run it directly:

```bash
gradle run
```

## Usage

### Starting the Local Server

The server uses STDIO for communication. You can run it directly using the installed distribution:

1.  **Generate the distribution**:
    ```bash
    gradle installDist
    ```
2.  **Run the binary**:
    ```bash
    ./build/install/news-mcp/bin/news-mcp
    ```
    *Note: The server will wait for JSON-RPC input on stdin. To test it manually, you would need to send valid MCP protocol messages.*

### Using from Jupyter Notebook

You can consume this MCP server in a Jupyter Notebook using the Python `mcp` SDK.

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
    # Replace the path with the absolute path to your news-mcp binary
    server_params = StdioServerParameters(
        command="/home/rvv/Downloads/ai-experiments/News-MCP/build/install/news-mcp/bin/news-mcp",
        args=[],
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
      "command": "/path/to/news-mcp/build/install/news-mcp/bin/news-mcp",
      "args": []
    }
  }
}
```

*Note: You may need to run `gradle installDist` first to create the executable in `build/install`.*

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
