import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from "@modelcontextprotocol/sdk/types.js";
import axios from "axios";

const BASE_URL = "http://localhost:8080/api";

const server = new Server(
  {
    name: "campus-security-mcp",
    version: "1.0.0",
  },
  {
    capabilities: {
      tools: {},
    },
  }
);

// Define tools
server.setRequestHandler(ListToolsRequestSchema, async () => {
  return {
    tools: [
      {
        name: "login",
        description: "登录校园安全教育平台，获取用户Token（需要教师账号）",
        inputSchema: {
          type: "object",
          properties: {
            username: { type: "string", description: "用户名" },
            password: { type: "string", description: "密码" },
          },
          required: ["username", "password"],
        },
      },
      {
        name: "publish_course",
        description: "发布安全课程（需提供教师Token）。如果需要内容补全，请先使用你自身的网络搜索能力查找资料再调用本接口",
        inputSchema: {
          type: "object",
          properties: {
            token: { type: "string", description: "登录获取的Token" },
            title: { type: "string", description: "课程标题" },
            category: { type: "string", description: "课程分类，如：校园防诈骗、消防安全等" },
            content: { type: "string", description: "课程正文内容" },
            quiz: { type: "string", description: "课程关联的小测验（JSON字符串格式，包含question, options, answer）" },
            coverUrl: { type: "string", description: "封面图URL（可选）" },
            videoUrl: { type: "string", description: "视频URL（可选）" },
            rewardPoints: { type: "number", description: "奖励积分，默认10" }
          },
          required: ["token", "title", "category", "content"],
        },
      },
      {
        name: "publish_exam",
        description: "发布在线测评（需提供教师Token）。如果需要题目，请先使用你自身的网络搜索能力查找资料再调用本接口",
        inputSchema: {
          type: "object",
          properties: {
            token: { type: "string", description: "登录获取的Token" },
            title: { type: "string", description: "测评标题" },
            description: { type: "string", description: "测评简介" },
            timeLimit: { type: "number", description: "答题时间限制（分钟），默认30" },
            totalScore: { type: "number", description: "总分，默认100" },
            passScore: { type: "number", description: "及格分，默认60" },
            questions: {
              type: "array",
              description: "题目列表",
              items: {
                type: "object",
                properties: {
                  content: { type: "string", description: "题干" },
                  type: { type: "string", description: "题目类型：SINGLE_CHOICE, MULTIPLE_CHOICE, JUDGE" },
                  options: { type: "string", description: "选项的JSON字符串数组格式，如 [\"A\", \"B\"]" },
                  answer: { type: "string", description: "答案（若是单选通常是数字索引的字符串，如 \"0\"，或字母）" },
                  score: { type: "number", description: "题目分值，默认10" }
                },
                required: ["content", "type", "options", "answer"]
              }
            }
          },
          required: ["token", "title", "description", "questions"],
        },
      }
    ],
  };
});

// Handle tool execution
server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;

  try {
    if (name === "login") {
      const response = await axios.post(`${BASE_URL}/user/login`, {
        username: args.username,
        password: args.password,
      });
      if (response.data.code === 200) {
        return {
          content: [{ type: "text", text: `登录成功！Token: ${response.data.data}` }],
        };
      } else {
        return {
          content: [{ type: "text", text: `登录失败: ${response.data.message}` }],
          isError: true,
        };
      }
    } else if (name === "publish_course") {
      const response = await axios.post(
        `${BASE_URL}/course/publish`,
        {
          title: args.title,
          category: args.category,
          content: args.content,
          quiz: args.quiz || "[]",
          coverUrl: args.coverUrl || "https://images.unsplash.com/photo-1532094349884-543bc11b234d?auto=format&fit=crop&w=600&q=80",
          videoUrl: args.videoUrl || "",
          rewardPoints: args.rewardPoints || 10,
        },
        {
          headers: { Authorization: `Bearer ${args.token}` },
        }
      );
      if (response.data.code === 200) {
        return {
          content: [{ type: "text", text: `课程发布成功！` }],
        };
      } else {
        return {
          content: [{ type: "text", text: `课程发布失败: ${response.data.message} (注意：必须是教师账号)` }],
          isError: true,
        };
      }
    } else if (name === "publish_exam") {
      const publishDTO = {
        exam: {
          title: args.title,
          description: args.description,
          timeLimit: args.timeLimit || 30,
          totalScore: args.totalScore || 100,
          passScore: args.passScore || 60,
          isMandatory: true,
        },
        questions: args.questions.map(q => ({
          content: q.content,
          type: q.type,
          options: q.options,
          answer: q.answer,
          score: q.score || 10
        }))
      };

      const response = await axios.post(
        `${BASE_URL}/exam/publish`,
        publishDTO,
        {
          headers: { Authorization: `Bearer ${args.token}` },
        }
      );
      if (response.data.code === 200) {
        return {
          content: [{ type: "text", text: `测评发布成功！` }],
        };
      } else {
        return {
          content: [{ type: "text", text: `测评发布失败: ${response.data.message} (注意：必须是教师账号)` }],
          isError: true,
        };
      }
    } else {
      return {
        content: [{ type: "text", text: `Unknown tool: ${name}` }],
        isError: true,
      };
    }
  } catch (error) {
    return {
      content: [
        {
          type: "text",
          text: `Error executing tool: ${error.response?.data?.message || error.message}`,
        },
      ],
      isError: true,
    };
  }
});

// Start the server
async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error("Campus Security MCP Server running on stdio");
}

main().catch((error) => {
  console.error("Fatal error in main():", error);
  process.exit(1);
});
