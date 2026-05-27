#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os
import sys
import json
import io
from typing import Any, Dict, List

# Windows 终端 UTF-8 编码修复
if sys.platform == 'win32':
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
    sys.stdin = io.TextIOWrapper(sys.stdin.buffer, encoding='utf-8', errors='replace')

class McpServer:
    """完整的MCP协议服务器实现（JSON RPC 2.0）"""
    
    def __init__(self):
        self.tools = {}
    
    def add_tool(self, func, input_schema=None):
        """注册工具函数"""
        tool_name = func.__name__
        description = func.__doc__ or ""
        if hasattr(func, '__tool_description__'):
            description = func.__tool_description__
        self.tools[tool_name] = {
            'function': func,
            'description': description,
            'inputSchema': input_schema or {"type": "object", "properties": {}}
        }
    
    def _get_tool_info(self) -> List[Dict[str, Any]]:
        """获取所有工具信息"""
        tools_info = []
        for name, info in self.tools.items():
            tools_info.append({
                'name': name,
                'description': info['description'],
                'inputSchema': info['inputSchema']
            })
        return tools_info
    
    def _handle_request(self, request: Dict[str, Any]) -> Dict[str, Any]:
        """处理单个请求"""
        request_id = request.get('id')
        jsonrpc = request.get('jsonrpc')
        method = request.get('method')
        params = request.get('params', {})
        
        if jsonrpc != '2.0':
            return {
                'jsonrpc': '2.0',
                'id': request_id,
                'error': {
                    'code': -32600,
                    'message': 'Invalid JSON RPC request'
                }
            }
        
        # MCP协议方法
        if method == 'initialize':
            return {
                'jsonrpc': '2.0',
                'id': request_id,
                'result': {
                    'protocolVersion': '2024-11-05',
                    'capabilities': {
                        'tools': {
                            'listChanged': True
                        }
                    },
                    'serverInfo': {
                        'name': 'jobai-mcp-server',
                        'version': '1.0.0',
                        'description': 'JobAI Automation MCP Server'
                    }
                }
            }
        
        elif method == 'describe':
            return {
                'jsonrpc': '2.0',
                'id': request_id,
                'result': {
                    'name': 'jobai-mcp-server',
                    'version': '1.0.0',
                    'description': 'JobAI Automation MCP Server',
                    'tools': self._get_tool_info()
                }
            }
        
        elif method == 'tools/list':
            return {
                'jsonrpc': '2.0',
                'id': request_id,
                'result': {
                    'tools': self._get_tool_info()
                }
            }

        elif method == 'call' or method == 'tools/call':
            if method == 'call':
                tool_name = params.get('name')
                arguments = params.get('arguments', {})
            else:
                tool_name = params.get('name')
                arguments = params.get('arguments', {})

            print(f"[MCP DEBUG] tools/call received - tool_name: {tool_name}, arguments: {arguments}", file=sys.stderr, flush=True)

            if tool_name not in self.tools:
                return {
                    'jsonrpc': '2.0',
                    'id': request_id,
                    'error': {
                        'code': -32601,
                        'message': f"Tool '{tool_name}' not found"
                    }
                }
            
            try:
                result = self.tools[tool_name]['function'](**arguments)
                print(f"[MCP DEBUG] Tool '{tool_name}' executed successfully, result length: {len(str(result))}", file=sys.stderr, flush=True)
                return {
                    'jsonrpc': '2.0',
                    'id': request_id,
                    'result': {
                        'content': [
                            {
                                'type': 'text',
                                'text': result if isinstance(result, str) else json.dumps(result)
                            }
                        ]
                    }
                }
            except Exception as e:
                print(f"[MCP DEBUG] Tool '{tool_name}' execution error: {e}", file=sys.stderr, flush=True)
                return {
                    'jsonrpc': '2.0',
                    'id': request_id,
                    'error': {
                        'code': -32603,
                        'message': str(e)
                    }
                }
        
        else:
            return {
                'jsonrpc': '2.0',
                'id': request_id,
                'error': {
                    'code': -32601,
                    'message': f"Method '{method}' not found"
                }
            }
    
    def run(self):
        """启动服务器主循环"""
        while True:
            try:
                line = sys.stdin.readline()
                if not line:
                    break
                
                line = line.strip()
                if not line:
                    continue
                
                request = json.loads(line)
                response = self._handle_request(request)
                print(json.dumps(response), flush=True)
                
            except json.JSONDecodeError as e:
                print(json.dumps({
                    'jsonrpc': '2.0',
                    'id': None,
                    'error': {
                        'code': -32700,
                        'message': f"Parse error: {str(e)}"
                    }
                }), flush=True)
            except Exception as e:
                print(json.dumps({
                    'jsonrpc': '2.0',
                    'id': None,
                    'error': {
                        'code': -32603,
                        'message': str(e)
                    }
                }), flush=True)

def Tool(description: str):
    """工具装饰器"""
    def decorator(func):
        func.__tool_description__ = description
        return func
    return decorator

@Tool(description="Read file content for getting conversation context or background information")
def read_file(path: str) -> str:
    """Read file content from mcp_context directory"""
    base_dir = "mcp_context"
    # 如果路径已经以 mcp_context/ 开头，则不再重复添加
    if path.startswith(base_dir + "/") or path.startswith(base_dir + "\\"):
        full_path = path
    else:
        full_path = os.path.join(base_dir, path)
        
    try:
        with open(full_path, 'r', encoding='utf-8') as f:
            return f.read()
    except FileNotFoundError:
        return f"Error: File not found: {full_path}"
    except Exception as e:
        return f"Error: {str(e)}"

@Tool(description="Write content to specified file for saving or updating conversation context")
def write_file(path: str, content: str) -> str:
    """Write content to file in mcp_context directory"""
    base_dir = "mcp_context"
    # 如果路径已经以 mcp_context/ 开头，则不再重复添加
    if path.startswith(base_dir + "/") or path.startswith(base_dir + "\\"):
        full_path = path
    else:
        full_path = os.path.join(base_dir, path)
        
    try:
        os.makedirs(os.path.dirname(full_path), exist_ok=True)
        with open(full_path, 'w', encoding='utf-8') as f:
            f.write(content)
        return f"Success: Content written to {path}"
    except Exception as e:
        return f"Error: {str(e)}"

@Tool(description="List files and subdirectories in specified directory")
def list_directory(path: str = "") -> List[str]:
    """List files in directory within mcp_context"""
    base_dir = "mcp_context"
    
    if not path:
        full_path = base_dir
    elif path.startswith(base_dir + "/") or path.startswith(base_dir + "\\"):
        full_path = path
    else:
        full_path = os.path.join(base_dir, path)
    
    try:
        if not os.path.exists(full_path):
            os.makedirs(full_path, exist_ok=True)
            
        entries = []
        for entry in os.listdir(full_path):
            entry_path = os.path.join(full_path, entry)
            if os.path.isdir(entry_path):
                entries.append(entry + "/")
            else:
                entries.append(entry)
        return entries
    except Exception as e:
        return [f"Error: {str(e)}"]

def main():
    """Start the MCP server"""
    os.makedirs("mcp_context", exist_ok=True)
    
    server = McpServer()
    
    # Read file schema
    read_file_schema = {
        "type": "object",
        "properties": {
            "path": {
                "type": "string",
                "description": "Path to the file to read within mcp_context"
            }
        },
        "required": ["path"]
    }
    
    # Write file schema
    write_file_schema = {
        "type": "object",
        "properties": {
            "path": {
                "type": "string",
                "description": "Path to the file to write within mcp_context"
            },
            "content": {
                "type": "string",
                "description": "Content to write to the file"
            }
        },
        "required": ["path", "content"]
    }
    
    # List directory schema
    list_directory_schema = {
        "type": "object",
        "properties": {
            "path": {
                "type": "string",
                "description": "Path to the directory to list within mcp_context (default: root)"
            }
        }
    }
    
    server.add_tool(read_file, read_file_schema)
    server.add_tool(write_file, write_file_schema)
    server.add_tool(list_directory, list_directory_schema)
    
    server.run()

if __name__ == "__main__":
    main()
