import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * このクラスは、入力TSVファイルを読み込み、データを集約し、結果を出力するプログラムです。
 * ヘッダー行を考慮して、各行のデータを適切にフォーマットします。
 */
public class DataAggregator {

    /** データをタブで分割 */
    private static final String SPLIT_DELIMITER = "\t";
    /** デフォルト区切り文字 */
    private static final String DEFAULT_DELIMITER = ":";
    /** splitメソッドの制限なし */
    private static final int NOT_LIMIT = -1;
    /** タブ区切りのパターン */
    private static Pattern spliterPattern = Pattern.compile(Pattern.quote(SPLIT_DELIMITER));
    /** デフォルト区切り文字のパターン */
    private static Pattern delimiterPattern = Pattern.compile(Pattern.quote(DEFAULT_DELIMITER));
    /** ヘッダー行を保持 */
    private static String[] header;

    /**
     * スプリッタのパターンを追加します。
     *
     * @param spliter スプリッタの正規表現
     */
    public static void addSpliter(String spliter) {
        spliterPattern = Pattern.compile(spliterPattern.pattern() + "|" + Pattern.quote(spliter));
    }

    /**
     * スプリッタのパターンを置き換えます。
     *
     * @param spliter 置き換えるスプリッタの正規表現
     */
    public static void replaceSpliter(String spliter) {
        spliterPattern = Pattern.compile(Pattern.quote(spliter));
    }

    /**
     * 区切り文字のパターンを追加します。
     *
     * @param delimiter 区切り文字の正規表現
     */
    public static void addDelimiter(String delimiter) {
        delimiterPattern = Pattern.compile(delimiterPattern.pattern() + "|" + Pattern.quote(delimiter));
    }

    /**
     * 区切り文字のパターンを置き換えます。
     *
     * @param delimiter 置き換える区切り文字の正規表現
     */
    public static void replaceDelimiter(String delimiter) {
        delimiterPattern = Pattern.compile(Pattern.quote(delimiter));
    }

    /**
     * プログラムのエントリーポイント。入力ファイルを読み込み、データを集約し、結果を出力ファイルに書き込みます。
     *
     * @param args コマンドライン引数。入力ファイルと出力ファイルのパスを含む
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("使用法: java DataAggregator <入力ファイル> <出力ファイル>");
            return;
        }

        // 例としてカンマを追加
        // addSpliter(",");
        // addDelimiter(",");

        // 入力ファイル名
        String inputFile = args[0];
        // 出力ファイル名
        String outputFile = args[1];

        // 読み込んだデータを格納するリスト
        List<String[]> data = new ArrayList<>();
        // 集約データを格納するマップ（キーはデータの最初の列）
        Map<String, List<String[]>> aggregatedData = new TreeMap<>();

        // データの読み込み
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            String line;
            System.out.println("入力:");
            while ((line = br.readLine()) != null) {
                // 行をタブで分割して配列に変換
                String[] parts = spliterPattern.split(line, NOT_LIMIT);
                if (header == null) {
                    // 1行目をヘッダーとして保存
                    header = parts;
                }
                if (parts.length < header.length) {
                    // 列が不足している場合、空文字列で埋める
                    String[] newParts = new String[header.length];
                    System.arraycopy(parts, 0, newParts, 0, parts.length);
                    for (int i = parts.length; i < newParts.length; i++) {
                        newParts[i] = "";
                    }
                    parts = newParts;
                } else if (parts.length > header.length) {
                    // 列が多すぎる場合、余分な列を削除
                    String[] newParts = new String[header.length];
                    System.arraycopy(parts, 0, newParts, 0, header.length);
                    parts = newParts;
                }
                // データをリストに追加
                data.add(parts);
                // 入力データをタブを視覚的に表示して出力
                System.out.println(line.replace("\t", "→"));
            }
        } catch (IOException e) {
            System.err.println("ファイルの読み込み中にエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
        }

        // データの読み込み後 (`data` リストに追加された状態):
        // [
        //     ["fruit", "apple", "green"],
        //     ["fruit", "banana", ""],
        //     ["beverage", "", ""],
        //     ["beverage", "coke", ""],
        //     ["pet", "dog", "loyal"]
        // ]

        // データの集約
        for (String[] row : data) {
            // 最初の列の値をキー（データの最初の列の値）として使用
            String key = row[0];
            if (!aggregatedData.containsKey(key)) {
                // マップにキーが存在しない場合、新しいリストを作成
                aggregatedData.put(key, new ArrayList<>());
            }
            // キーに対応するリストに行データを追加（行全体を列データとして追加）
            aggregatedData.get(key).add(row);
        }

        // データの集約後 (`aggregatedData` マップに追加された状態):
        // {
        //     "fruit": [
        //         ["fruit", "apple", "green"],
        //         ["fruit", "banana", ""]
        //     ],
        //     "beverage": [
        //         ["beverage", "", ""],
        //         ["beverage", "coke", ""]
        //     ],
        //     "pet": [
        //         ["pet", "dog", "loyal"]
        //     ]
        // }

        // 結果の生成
        List<Map.Entry<String, List<String[]>>> result = new ArrayList<>(aggregatedData.entrySet());


        List<String> output = new ArrayList<>();
        for (Map.Entry<String, List<String[]>> entry : result) {
            // マップのキー（データの最初の列の値）
            String key = entry.getKey();
            // キーに対応する行のリスト（行全体を列データとして追加）
            List<String[]> rows = entry.getValue();
            StringBuilder sb = new StringBuilder(key);

            // 最大列数を取得
            int maxColumns = rows.stream().mapToInt(r -> r.length).max().orElse(0);

            for (int col = 1; col < maxColumns; col++) {
                StringBuilder colValues = new StringBuilder();
                for (int i = 0; i < rows.size(); i++) {
                    String[] row = rows.get(i);
                    if (i > 0) {
                        // 区切り文字を追加
                        colValues.append(DEFAULT_DELIMITER);
                    }
                    colValues.append(col < row.length ? (row[col].isEmpty() ? "" : row[col]) : "");
                }
                sb.append(SPLIT_DELIMITER).append(colValues.toString());
            }
            output.add(sb.toString());
        }

        // 結果の生成後 (`output` リストに追加された状態):
        // [
        //     "beverage→:coke→:",
        //     "fruit→apple:banana→green:",
        //     "pet→dog→loyal"
        // ]

        // 結果の出力
        System.out.println("\n出力:");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
            for (String line : output) {
                bw.write(line);
                bw.newLine();
                System.out.println(line.replace("\t", "→"));
            }
        } catch (IOException e) {
            System.err.println("ファイルの書き込み中にエラーが発生しました: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
