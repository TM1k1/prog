import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * このクラスは、入力TSVファイルを第一正規化し、結果を重複なしで出力するプログラムです。
 * 標準ライブラリを使用しています。 
 */
public class NormalizeCSV {
    // 区切り文字の初期定義
    private static final String SPLIT_DELIMITER = "\t";
    // デフォルト区切り文字
    private static final String DEFAULT_DELIMITER = ":";
    // splitメソッドの制限なし
    private static final int NOT_LIMIT = -1;
    // デフォルト区切り文字のパターン
    private static Pattern delimiterPattern = Pattern.compile(Pattern.quote(DEFAULT_DELIMITER));
    // タブ区切りのパターン
    private static Pattern spliterPattern = Pattern.compile(Pattern.quote(SPLIT_DELIMITER));

    /**
     * 区切り文字のパターンを追加します
     * @param delimiter 区切り文字の正規表現
     */
    public static void addDelimiter(String delimiter) {
        delimiterPattern = Pattern.compile(delimiterPattern.pattern() + "|" + Pattern.quote(delimiter));
    }

    /**
     * スプリッタのパターンを追加します
     * @param spliter スプリッタの正規表現
     */
    public static void addSpliter(String spliter) {
        spliterPattern = Pattern.compile(spliterPattern.pattern() + "|" + Pattern.quote(spliter));
    }

    /**
     * 区切り文字のパターンを置き換えます
     * @param delimiter 置き換える区切り文字の正規表現
     */
    public static void replaceDelimiter(String delimiter) {
        delimiterPattern = Pattern.compile(Pattern.quote(delimiter));
    }

    /**
     * スプリッタのパターンを置き換えます
     * @param spliter 置き換えるスプリッタの正規表現
     */
    public static void replaceSpliter(String spliter) {
        spliterPattern = Pattern.compile(Pattern.quote(spliter));
    }

    /**
     * 入力TSVファイルを第一正規化し、結果を出力するメソッドです。
     * 
     * @param inputFile 入力TSVファイルのパス
     * @param outputFile 出力TSVファイルのパス
     * @throws IOException 入出力エラーが発生した場合にスローされます。
     */
    public static void normalize(String inputFile, String outputFile) throws IOException {
        // 入力ファイルを読み取るためのBufferedReaderを作成
        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        // 正規化された行を保持するためのセット（重複を防止、順序を保持するためLinkedHashSetを使用）
        Set<String> normalizedRows = new LinkedHashSet<>();
        // データの最大列数を保存する変数
        int columnCount = 0;

        String line;
        String[] workingMem = null;
        // 入力データサンプル
        // apple→fruit:sale
        // banana:cherry→fruit
        // →beverage
        // 1行目を読み取り、最大列数を取得
        System.out.println("入力:");
        if ((line = reader.readLine()) != null) {
            // 標準出力に表示（区切り文字を視覚化）
            System.out.println(spliterPattern.matcher(line).replaceAll("→"));
            // 空文字を含むように分割
            String[] columns = spliterPattern.split(line, NOT_LIMIT);
            // 1行目の列数を最大列数とする
            columnCount = columns.length;
            workingMem = new String[columnCount];
            // 初期化時に全ての要素を空文字列にする
            for (int i = 0; i < workingMem.length; i++) {
                workingMem[i] = "";
            }
            // 正規化処理を行い、LinkedHashSetに追加
            normalizeRow(columns, 0, workingMem, normalizedRows);
        }

        // データの遷移例（1行目の処理後）:
        // normalizedRows = {"apple\tfruit\tsale"}

        // 2行目以降の行を読み取り正規化
        while ((line = reader.readLine()) != null) {
            // 標準出力に表示（区切り文字を視覚化）
            System.out.println(spliterPattern.matcher(line).replaceAll("→"));
            // 空文字を含むように分割
            String[] columns = spliterPattern.split(line, NOT_LIMIT);
            // 1行目の列数に合わせて列数を調整
            if (columns.length > columnCount) {
                // 列数を最大列数に合わせる
                columns = Arrays.copyOf(columns, columnCount);
            }
            workingMem = new String[columnCount];
            // 初期化時に全ての要素を空文字列にする
            for (int i = 0; i < workingMem.length; i++) {
                workingMem[i] = "";
            }
            // 正規化処理を行い、LinkedHashSetに追加
            normalizeRow(columns, 0, workingMem, normalizedRows);
        }

        // データの遷移例（全行の処理後）:
        // キー  値
        // apple  fruit   sale
        // banana fruit   
        // cherry fruit   
        // beverage       

        // normalizedRows = {
        //     "apple\tfruit\tsale",
        //     "banana\tfruit\t",
        //     "cherry\tfruit\t",
        //     "\tbeverage\t"
        // }

        System.out.println("\n出力:");
        // 結果をファイルに書き込む
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            for (String row : normalizedRows) {
                // 各行を出力
                writer.write(row);
                writer.newLine();
                // 標準出力に表示（区切り文字を視覚化）
                System.out.println(spliterPattern.matcher(row).replaceAll("→"));
            }
        }
    }

    /**
     * 行を正規化し、結果をセットに追加するメソッドです。
     * このメソッドは再帰的に呼び出され、すべての可能な組み合わせを生成します。
     *
     * @param columns 現在の列の配列
     * @param columnIndex 現在の列のインデックス
     * @param current 現在の値の組み合わせを保持する配列
     * @param normalizedRows 正規化された行を保持するセット
     */
    private static void normalizeRow(String[] columns, int columnIndex, String[] current, Set<String> normalizedRows) {
        if (columnIndex == columns.length) {
            // 全ての列を処理し終えたら、セットに追加
            normalizedRows.add(String.join(SPLIT_DELIMITER, current));
        } else {
            // 現在の列の値を指定されたパターンで分割
            String[] values = delimiterPattern.split(columns[columnIndex], NOT_LIMIT);
            // 各値について再帰的に処理
            for (String value : values) {
                // 空文字の場合はそのまま空文字として処理  
                current[columnIndex] = value;
                // 再帰的に次の列を処理
                normalizeRow(columns, columnIndex + 1, current, normalizedRows);
            }
        }
    }

    /**
     * メインメソッド。入力ファイルと出力ファイルのパスを引数として受け取り、正規化処理を実行します。
     * 必要に応じて追加の区切り文字も設定します。
     *
     * @param args 入力TSVファイルのパスと出力TSVファイルのパス
     * @throws IOException 入出力エラーが発生した場合にスローされます。
     */
    public static void main(String[] args) throws IOException {
        // 引数の数を確認
        if (args.length != 2) {
            System.err.println("使用方法: java NormalizeCSV <入力ファイル> <出力ファイル>");
            System.exit(1);
        }

        // 必要に応じて追加の区切り文字を設定
        // 例としてカンマを追加
        // addDelimiter(",");
        // addSpliter(",");

        // 正規化処理の実行
        normalize(args[0], args[1]);
    }
}
