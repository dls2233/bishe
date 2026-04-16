import http.client
import json
import threading
import time

# 测试配置
TOTAL_USERS = 1000       # 模拟的在线用户数
BASE_URL = "localhost:8080"
TOKEN_PREFIX = "test_token_user_" # 为了压测，假设后端临时允许任何token或你有一个万能token
# 注意：实际测试中，如果后端有严格的 JWT 校验，你需要先用一个脚本批量注册/登录获取 1000 个真实的 JWT Token

connected_count = 0
failed_count = 0
lock = threading.Lock()

def sse_client(user_id):
    global connected_count, failed_count
    try:
        # 建立长连接
        conn = http.client.HTTPConnection(BASE_URL)
        
        # 为了演示，我们假设后端临时放开压测 token 的校验
        # 如果有真实的 JWT，请在这里换成真实的 token
        token = f"MOCK_TOKEN_{user_id}" 
        url = f"/api/sse/connect?token={token}"
        
        conn.request("GET", url)
        response = conn.getresponse()
        
        if response.status == 200:
            with lock:
                connected_count += 1
            
            # 持续读取数据流，保持连接不断开
            while True:
                chunk = response.read(1024)
                if not chunk:
                    break
        else:
            with lock:
                failed_count += 1
                
    except Exception as e:
        with lock:
            failed_count += 1

print(f"开始启动 {TOTAL_USERS} 个 SSE 客户端并发连接...")
threads = []

# 启动 1000 个线程模拟 1000 个客户端
start_time = time.time()
for i in range(TOTAL_USERS):
    t = threading.Thread(target=sse_client, args=(i,))
    t.daemon = True
    t.start()
    threads.append(t)
    
    # 稍微加一点延迟，防止瞬间打满把本地网卡端口耗尽
    if i % 50 == 0:
        time.sleep(0.1)

print("所有线程已启动，等待连接建立...")
time.sleep(5) # 等待几秒钟让连接全部建立

print("="*40)
print(f"并发连接建立结果：")
print(f"成功连接数: {connected_count}")
print(f"失败连接数: {failed_count}")
print(f"耗时: {time.time() - start_time:.2f} 秒")
print("="*40)

if connected_count > 0:
    print("\n现在你可以尝试调用后端的广播接口，看看这 1000 个连接是否正常接收。")
    print("按 Ctrl+C 结束测试...")
    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print("\n压测结束。")
