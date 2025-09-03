package FPS.Watcher;

import static FPS.Watcher.FPSWatchService.getFPSRecordFolder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.LruCache;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ManageRecordActivity extends Activity {

    public static class CsvFile {
        public final String date;  // 记录日期
        public final String appName;  // APP包名
        public final String appLabel;  // APP名称
        public final File file;  // 对应的CSV文件

        public CsvFile(String date, String appName, String appLabel, File file) {
            this.date = date;
            this.appName = appName;
            this.appLabel = appLabel;
            this.file = file;
        }


        // 设置用于排序的比较器
        public static Comparator<CsvFile> byDate = Comparator.comparing(o -> o.date);

        public static Comparator<CsvFile> byAppLabel = Comparator.comparing(o -> o.appLabel);
    }
    public static class AppIconLoader {
        private final LruCache<String, Drawable> iconCache;
        private final Set<String> loadingSet = Collections.synchronizedSet(new HashSet<>());
        private final Map<String, List<ImageView>> pendingViews = new ConcurrentHashMap<>();

        private final ExecutorService executor = Executors.newFixedThreadPool(4);
        private final PackageManager pm;

        public AppIconLoader(Context context) {
            pm = context.getPackageManager();
            iconCache = new LruCache<>(50); // 缓存 50 个图标
        }

        public interface Callback {
            void onIconLoaded(Drawable icon);
        }

        public void load(String packageName, ImageView imageView, int placeholderResId, Callback callback) {
            Drawable cachedIcon = iconCache.get(packageName);
            if (cachedIcon != null) {
                imageView.setImageDrawable(cachedIcon);
                if (callback != null) callback.onIconLoaded(cachedIcon);
                return;
            }

            imageView.setImageResource(placeholderResId);

            // 添加到等待队列
            pendingViews.computeIfAbsent(packageName, k -> new ArrayList<>()).add(imageView);

            if (loadingSet.contains(packageName)) return;

            loadingSet.add(packageName);

            executor.execute(() -> {
                Drawable icon = getAppIcon(packageName);
                if (icon != null) {
                    iconCache.put(packageName, icon);
                }
                loadingSet.remove(packageName);

                // 获取等待刷新的 ImageView 列表
                List<ImageView> views = pendingViews.remove(packageName);
                if (views != null) {
                    for (ImageView v : views) {
                        v.post(() -> {
                            Object tag = v.getTag();
                            if (tag != null && tag.equals(packageName)) {
                                v.setImageDrawable(icon != null ? icon : v.getContext().getDrawable(placeholderResId));
                                if (callback != null && icon != null) {
                                    callback.onIconLoaded(icon);
                                }
                            }
                        });
                    }
                }
            });
        }


        private Drawable getAppIcon(String packageName) {
            try {
                ApplicationInfo applicationInfo = pm.getApplicationInfo(packageName, 0);
                return applicationInfo.loadIcon(pm);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    public static String getAppLabelByName(PackageManager pm, String mWatchingPackageName) {
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(mWatchingPackageName, 0);
            return pm.getApplicationLabel(appInfo).toString();
        } catch (Throwable e) {
            return mWatchingPackageName;
        }
    }

    ArrayAdapter<CsvFile> adapter;
    List<CsvFile> csvFiles;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(getString(R.string.manage_record));

        File recordFolder = getFPSRecordFolder(this);
        if (!recordFolder.exists()) {
            recordFolder.mkdirs();
        } else if (recordFolder.isFile()) {
            recordFolder.delete();
            recordFolder.mkdirs();
        }

        File[] files = recordFolder.listFiles((dir1, filename) -> filename.endsWith(".csv"));
        csvFiles = new ArrayList<>();

        PackageManager pm = getPackageManager();
        for (File file : files) {
            String fileName = file.getName();
            // 提取日期和APP名称
            String[] parts = fileName.split("_");
            if (parts.length == 2) {
                String date = parts[0];  // 记录日期
                String appName = parts[1].replace(".csv", "");  // APP名称
                String appLabel = getAppLabelByName(pm,appName);
                csvFiles.add(new CsvFile(date, appName, appLabel, file));
            }
        }

        SharedPreferences sp = getSharedPreferences("s", 0);
        mCurrentSortTypeIsByLabel = sp.getBoolean("sort_type",true);
        // 按日期排序
        csvFiles.sort(mCurrentSortTypeIsByLabel ?CsvFile.byAppLabel : CsvFile.byDate);

        ListView listView = new ListView(this);
        setContentView(listView); // 或者 add 到其他布局

        AppIconLoader iconLoader = new AppIconLoader(this);


        adapter = new ArrayAdapter<>(this, android.R.layout.activity_list_item, android.R.id.text1, csvFiles) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    LinearLayout layout = new LinearLayout(getContext());
                    layout.setOrientation(LinearLayout.HORIZONTAL);
                    layout.setPadding(16, 16, 16, 16);

                    ImageView icon = new ImageView(getContext());
                    icon.setId(android.R.id.icon);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(120, 120);
                    params.leftMargin = 40;
                    params.rightMargin = 40;
                    params.topMargin = 10;
                    params.bottomMargin = 10;
                    icon.setLayoutParams(params);
                    layout.addView(icon);

                    LinearLayout textLayout = new LinearLayout(getContext());
                    textLayout.setOrientation(LinearLayout.VERTICAL);
                    textLayout.setPadding(16, 0, 0, 0);

                    TextView title = new TextView(getContext());
                    title.setId(android.R.id.text1);
                    title.setTypeface(Typeface.DEFAULT_BOLD);
                    title.setTextColor(getColor(R.color.colorAccent));
                    title.setTextSize(17);

                    TextView subtitle = new TextView(getContext());
                    subtitle.setId(android.R.id.text2);
                    subtitle.setTextSize(14);
                    subtitle.setTextColor(Color.GRAY);

                    textLayout.addView(title);
                    textLayout.addView(subtitle);

                    layout.addView(textLayout);
                    convertView = layout;
                }

                CsvFile file = getItem(position);
                ((TextView) convertView.findViewById(android.R.id.text1)).setText(file.appLabel);
                ((TextView) convertView.findViewById(android.R.id.text2)).setText(file.date);
                convertView.findViewById(android.R.id.icon).setTag(file.appName);
                iconLoader.load(file.appName,(convertView.findViewById(android.R.id.icon)), R.drawable.ic_tile, null);

                return convertView;
            }

        };
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            CsvFile selectedFile = adapter.getItem(position);
            if (selectedFile != null) {
                // 创建 Intent 跳转到目标 Activity
                Intent intent = new Intent(this, RecordDetailActivity.class);

                // 创建 Bundle 来传递 CsvFile 对象
                Bundle bundle = new Bundle();
                bundle.putSerializable("csvFile", selectedFile.file);  // 将 File 对象传递过去
                intent.putExtras(bundle);

                // 启动新的 Activity
                startActivity(intent);
            }
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            CsvFile selectedFile = adapter.getItem(position);
            if (selectedFile != null) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.confirm_delete_title)
                        .setMessage(String.format(getString(R.string.confirm_delete_content), selectedFile.file.getName()))
                        .setPositiveButton(R.string.delete, (dialog, which) -> {
                            csvFiles.remove(position);         // 删除数据
                            adapter.notifyDataSetChanged();    // 通知刷新 UI
                            selectedFile.file.delete();
                            Toast.makeText(this, R.string.deleted, Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }
            return true; // 返回 true 表示事件已处理
        });


    }

    public boolean mCurrentSortTypeIsByLabel;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 创建菜单项
        MenuItem sortItem = menu.add(Menu.NONE, 1, Menu.NONE, mCurrentSortTypeIsByLabel ? getString(R.string.sort_by_date) : getString(R.string.sort_by_label));
        sortItem.setIcon(android.R.drawable.ic_menu_sort_by_size);
        sortItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            mCurrentSortTypeIsByLabel = !mCurrentSortTypeIsByLabel;
            csvFiles.sort(mCurrentSortTypeIsByLabel ? CsvFile.byAppLabel : CsvFile.byDate);
            adapter.notifyDataSetChanged();
            SharedPreferences sp = getSharedPreferences("s", 0);
            sp.edit().putBoolean("sort_type", mCurrentSortTypeIsByLabel).apply();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem sortItem = menu.findItem(1);
        if (sortItem != null) {
            sortItem.setTitle(mCurrentSortTypeIsByLabel ? getString(R.string.sort_by_date) : getString(R.string.sort_by_label));
        }
        return super.onPrepareOptionsMenu(menu);
    }

}
