# Campus Security MCP Server

这是一个标准 MCP (Model Context Protocol) Server，专为教师端快速发布课程与测评而设计。通过将本服务配置到支持 MCP 的 IDE 或大模型客户端（如 Trae、Cursor、Claude Desktop）中，AI 即可根据指令直接在系统后台创建课程和试卷。

## 如何使用

1. 安装依赖：
```bash
cd /Users/bytedance/GolandProjects/bishe/mcp-server
npm install
```

2. 启动服务进行测试：
```bash
node index.js
```

## 在 Trae 或 Cursor 中配置

打开客户端的 MCP 配置文件（例如 Trae 的 Settings -> MCP），添加如下配置：

```json
{
  "mcpServers": {
    "campus-security": {
      "command": "node",
      "args": [
        "/Users/bytedance/GolandProjects/bishe/mcp-server/index.js"
      ]
    }
  }
}
```

## 支持的工具列表 (Tools)

1. `login(username, password)`
   - 登录系统获取教师权限的 Token。必须是带有 `TEACHER` 或 `ADMIN` 权限的账户。
2. `publish_course(token, title, category, content, quiz, ...)`
   - 自动发布课程。AI 会利用自身的联网搜索能力帮你搜集课程正文和课后测试题，然后调用此接口直接入库。
3. `publish_exam(token, title, description, questions, ...)`
   - 自动发布在线测评。AI 会生成题库（包含单选、多选、判断），并配置正确的 JSON 格式后提交给后端接口。
