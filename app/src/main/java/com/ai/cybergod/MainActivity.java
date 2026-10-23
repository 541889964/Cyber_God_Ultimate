package com.ai.cybergod;
import android.app.AlertDialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    private RecyclerView rvChat;
    private ChatAdapter adapter;
    private List<String[]> messages = new ArrayList<>();
    private EditText etMsg, etKey;
    private Spinner spinnerMode, spinnerModel, spinnerEngine;
    private TextView tvSysInfo;
    private LinearLayout layoutChips;
    private SharedPreferences sp;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final OkHttpClient client = new OkHttpClient.Builder().readTimeout(180, java.util.concurrent.TimeUnit.SECONDS).build();
    
    private int currentPermMode = 1;
    private int currentEngine = 0;
    private String currentModel = "deepseek-chat";
    private String customSystemPrompt = "你是安卓超级AI。执行系统操作用：[EXEC] 命令 或 [COMPILE] 语言\n代码\n[/COMPILE]。任务未完成禁止输出 [TASK_DONE]。";
    private boolean isUserScrolling = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sp = getSharedPreferences("CyberGod", Context.MODE_PRIVATE);

        rvChat = findViewById(R.id.rvChat);
        etMsg = findViewById(R.id.etMsg);
        etKey = findViewById(R.id.etKey);
        spinnerMode = findViewById(R.id.spinnerMode);
        spinnerModel = findViewById(R.id.spinnerModel);
        spinnerEngine = findViewById(R.id.spinnerEngine);
        tvSysInfo = findViewById(R.id.tvSysInfo);
        layoutChips = findViewById(R.id.layoutChips);
        
        Button btnSend = findViewById(R.id.btnSend);
        Button btnSaveKey = findViewById(R.id.btnSaveKey);
        Button btnSettings = findViewById(R.id.btnSettings);

        adapter = new ChatAdapter(this, messages);
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(adapter);

        rvChat.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrollStateChanged(RecyclerView r, int newState) {
                isUserScrolling = (newState == RecyclerView.SCROLL_STATE_DRAGGING);
            }
        });

        etKey.setText(sp.getString("api_key", ""));
        addMessage("System", "**Cyber God** 已启动。\n10项新功能已注入。");

        updateSysInfo();

        String[] modes = {"普通沙盒", "Root 提权", "Shizuku"};
        spinnerMode.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, modes));
        spinnerMode.setSelection(1);
        spinnerMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { currentPermMode = pos; }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        String[] engines = {"本地编译", "云端沙盒", "GitHub Actions"};
        spinnerEngine.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, engines));
        spinnerEngine.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { currentEngine = pos; }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        String[] models = {"V3 极速", "R1 深思"};
        spinnerModel.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, models));
        spinnerModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { currentModel = (pos == 0) ? "deepseek-chat" : "deepseek-reasoner"; }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        btnSaveKey.setOnClickListener(v -> { sp.edit().putString("api_key", etKey.getText().toString().trim()).apply(); Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show(); });

        // 10. 自定义系统提示词弹窗
        btnSettings.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("自定义 System Prompt");
            final EditText input = new EditText(this);
            input.setText(customSystemPrompt);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            builder.setView(input);
            builder.setPositiveButton("保存", (dialog, which) -> {
                customSystemPrompt = input.getText().toString();
                Toast.makeText(this, "AI 灵魂已重塑", Toast.LENGTH_SHORT).show();
            });
            builder.setNegativeButton("取消", null);
            builder.show();
        });

        addQuickChip("清理内存", "sync; echo 3 > /proc/sys/vm/drop_caches");
        addQuickChip("查看IP", "ifconfig");
        // 2. 一键抓取 Logcat
        addQuickChip("抓取日志", "logcat -d -t 100");
        // 3. 应用列表提取
        addQuickChip("获取APP包名", "pm list packages -3");
        // 5. 网络测速
        addQuickChip("网络测速", "ping -c 3 8.8.8.8");

        btnSend.setOnClickListener(v -> {
            String msg = etMsg.getText().toString().trim();
            if (msg.isEmpty()) return;
            etMsg.setText("");
            addMessage("Me", msg);
            callAPI(msg, null, false);
        });
    }

    private void updateSysInfo() {
        uiHandler.postDelayed(() -> {
            BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
            int battery = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            tvSysInfo.setText("🔋 电量: " + battery + "%  |  Android " + Build.VERSION.RELEASE + "  |  ⚙️ Engine: " + (currentEngine == 0 ? "本地" : currentEngine == 1 ? "沙盒" : "云"));
            updateSysInfo();
        }, 5000);
    }

    private void addQuickChip(String title, String cmd) {
        Button chip = new Button(this);
        chip.setText(title);
        chip.setTextSize(11);
        chip.setBackgroundColor(0xFF374151);
        chip.setTextColor(0xFF00FFCC);
        chip.setOnClickListener(v -> {
            addMessage("Me", "执行快捷指令: " + title);
            executeTerminal(cmd, "快捷指令");
        });
        layoutChips.addView(chip);
    }

    private void addMessage(String role, String content) {
        uiHandler.post(() -> {
            // 9. 智能上下文压缩 (保留最新 20 条)
            if (messages.size() > 20) {
                messages.remove(0);
                adapter.notifyItemRemoved(0);
            }
            messages.add(new String[]{role, content});
            adapter.notifyItemInserted(messages.size() - 1);
            if (!isUserScrolling) rvChat.scrollToPosition(messages.size() - 1);
        });
    }

    private void updateLastMessage(String append) {
        uiHandler.post(() -> {
            if (messages.isEmpty()) return;
            String[] last = messages.get(messages.size() - 1);
            if (last[0].startsWith("Agent")) {
                last[1] += append; adapter.notifyItemChanged(messages.size() - 1);
                if (!isUserScrolling) rvChat.scrollToPosition(messages.size() - 1);
            }
        });
    }

    private void callAPI(String question, String execResult, boolean isLoop) {
        String key = sp.getString("api_key", "");
        if (key.isEmpty()) { addMessage("System", "Key未配置"); return; }
        if (!isLoop) addMessage("Agent", "⏳ 思考中...\n");

        try {
            JSONObject json = new JSONObject();
            json.put("model", currentModel);
            json.put("stream", true);
            JSONArray msgs = new JSONArray();
            
            msgs.put(new JSONObject().put("role", "system").put("content", customSystemPrompt));
            msgs.put(new JSONObject().put("role", "user").put("content", question));
            if (isLoop && execResult != null) {
                msgs.put(new JSONObject().put("role", "assistant").put("content", "执行反馈：\n" + execResult + "\n请继续操作或修复错误。"));
            }
            json.put("messages", msgs);

            Request req = new Request.Builder().url("https://api.deepseek.com/v1/chat/completions")
                .header("Authorization", "Bearer " + key)
                .post(RequestBody.create(json.toString(), MediaType.parse("application/json"))).build();

            client.newCall(req).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) { updateLastMessage("\n[网络断开]"); }
                @Override public void onResponse(Call call, Response response) {
                    if (!response.isSuccessful() || response.body() == null) { updateLastMessage("\n[API异常]"); return; }
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()))) {
                        String line; StringBuilder full = new StringBuilder();
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("data: ") && !line.equals("data: [DONE]")) {
                                JSONObject chunk = new JSONObject(line.substring(6));
                                JSONObject delta = chunk.getJSONArray("choices").getJSONObject(0).optJSONObject("delta");
                                if (delta != null && delta.has("reasoning_content")) {
                                    updateLastMessage(delta.getString("reasoning_content"));
                                }
                                if (delta != null && delta.has("content") && !delta.getString("content").isEmpty()) {
                                    String c = delta.getString("content");
                                    updateLastMessage(c); full.append(c);
                                }
                            }
                        }
                        executeAction(full.toString(), question);
                    } catch (Exception e) {}
                }
            });
        } catch (Exception e) {}
    }

    private void executeAction(String reply, String originalQuestion) {
        if (reply.contains("[TASK_DONE]")) { addMessage("System", "✅ 目标达成，闭环结束！"); return; }

        if (reply.contains("[EXEC]")) {
            int start = reply.indexOf("[EXEC]") + 6;
            int end = reply.indexOf("\n", start);
            String cmd = (end == -1 ? reply.substring(start) : reply.substring(start, end)).replace("`", "").trim();
            
            if (ProEngine.isDangerous(cmd)) {
                uiHandler.post(() -> {
                    new AlertDialog.Builder(this)
                        .setTitle("⚠️ 高危拦截")
                        .setMessage("AI 试图执行：\n" + cmd + "\n是否放行？")
                        .setPositiveButton("放行", (d, w) -> executeTerminal(cmd, originalQuestion))
                        .setNegativeButton("拒绝", (d, w) -> callAPI(originalQuestion, "拒绝执行", true))
                        .show();
                });
                return;
            }
            executeTerminal(cmd, originalQuestion);
        }

        if (reply.contains("[COMPILE]") && reply.contains("[/COMPILE]")) {
            int start = reply.indexOf("[COMPILE]") + 9;
            int end = reply.indexOf("[/COMPILE]");
            String raw = reply.substring(start, end).trim();
            String lang = raw.startsWith("python") ? "python" : raw.startsWith("c") ? "c" : "sh";
            String code = raw.substring(raw.indexOf("\n")).replace("`", "").trim();

            addMessage("System", ">>> 启动 [" + lang + "] 编译器...");
            new Thread(() -> {
                String res = "";
                if (currentEngine == 0) {
                    // 7. 代码快照
                    File f = new File("/sdcard/AI_Workspace/snapshot_" + System.currentTimeMillis() + "." + (lang.equals("python") ? "py" : lang.equals("c") ? "c" : "sh"));
                    f.getParentFile().mkdirs();
                    try (FileWriter w = new FileWriter(f)) { w.write(code); } catch (Exception e) {}
                    
                    String cmd = lang.equals("python") ? "python " + f.getAbsolutePath() : lang.equals("c") ? "gcc " + f.getAbsolutePath() + " -o /sdcard/AI_Workspace/a.out && /sdcard/AI_Workspace/a.out" : "sh " + f.getAbsolutePath();
                    res = ProEngine.run(currentPermMode, cmd);
                } else {
                    res = "云端编译成功 (Mock)。请自行查看控制台输出。";
                }
                addMessage("System", ">>> 编译结果:\n" + res);
                callAPI(originalQuestion, res, true);
            }).start();
        }
    }

    private void executeTerminal(String cmd, String originalQuestion) {
        addMessage("System", ">>> 正在运行: " + cmd);
        new Thread(() -> {
            String res = ProEngine.run(currentPermMode, cmd);
            addMessage("System", ">>> 结果:\n" + res);
            callAPI(originalQuestion, res, true);
        }).start();
    }
}
