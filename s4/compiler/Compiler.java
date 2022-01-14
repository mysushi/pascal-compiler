package enshud.s4.compiler;

import java.util.LinkedList;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import enshud.casl.CaslSimulator;

public class Compiler {
	public LinkedList<String> key = new LinkedList<String>();
	public LinkedList<Integer> linenumber = new LinkedList<Integer>();
	public LinkedList<String> raw = new LinkedList<String>();
	public LinkedList<String> subprogramnames = new LinkedList<String>();
	public LinkedList<String> caslcode;
	public LinkedList<LinkedList<String>> caslcodes = new LinkedList<LinkedList<String>>();
	public LinkedList<String> constants = new LinkedList<String>();

	String[][] names = new String[100][100];
	int[][] attribute = new int[100][100];
	int varindex = 0;
	int arrayvarindex = 0;

	String[][] arraynames = new String[100][100];
	int[][] arraymin = new int[100][100];
	int[][] arraymax = new int[100][100];
	String[][] arraytype = new String[100][100];

	String[][] tempnames = new String[100][100];
	String[][] temptype = new String[100][100];
	int tempvarindex = 0;

	boolean synerr = false;
	boolean semerr = false;
	boolean wrtstr = false;
	boolean left = false;
	boolean subproc = false;
	boolean insubscript = false;
	int errlinenumber;
	int firstsynerrlinenumber;
	int firstsemerrlinenumber;
	int current = -1;
	int state = 0;
	int charnum = 0;
	int charnum2 = 0;
	int loop = 0;
	int fork = -1;
	int proc = 0;


	/**
	 * サンプルmainメソッド．
	 * 単体テストの対象ではないので自由に改変しても良い．
	 */
	public static void main(final String[] args) {
		// Compilerを実行してcasを生成する
		new Compiler().run("data/ts/normal06.ts", "tmp/out.cas");

		// 上記casを，CASLアセンブラ & COMETシミュレータで実行する
		CaslSimulator.run("tmp/out.cas", "tmp/out.ans");
	}

	/**
	 * TODO
	 * 
	 * 開発対象となるCompiler実行メソッド．
	 * 以下の仕様を満たすこと．
	 * 
	 * 仕様:
	 * 第一引数で指定されたtsファイルを読み込み，CASL IIプログラムにコンパイルする．
	 * コンパイル結果のCASL IIプログラムは第二引数で指定されたcasファイルに書き出すこと．
	 * 構文的もしくは意味的なエラーを発見した場合は標準エラーにエラーメッセージを出力すること．
	 * （エラーメッセージの内容はChecker.run()の出力に準じるものとする．）
	 * 入力ファイルが見つからない場合は標準エラーに"File not found"と出力して終了すること．
	 * 
	 * @param inputFileName 入力tsファイル名
	 * @param outputFileName 出力casファイル名
	 */
	public void run(final String inputFileName, final String outputFileName) {
		FileReader fr;
		FileWriter fw;
		BufferedReader br;
		BufferedWriter bw;
		try {
			fr = new FileReader(inputFileName);
			fw = new FileWriter(outputFileName);
			br = new BufferedReader(fr);
			bw = new BufferedWriter(fw);

			String line = br.readLine();
			String[] linesplit;
			do {
				linesplit = line.split("\t");
				raw.add(linesplit[0]);
				key.add(linesplit[1]);
				linenumber.add(Integer.parseInt(linesplit[3]));
				line = br.readLine();
			} while (line != null);

			program();

			if (synerr) {
				System.err.print("Syntax error: line ");
				System.err.println(firstsynerrlinenumber);
			} else if (semerr) {
				System.err.print("Semantic error: line ");
				System.err.println(firstsemerrlinenumber);
			} else {
//				System.out.println(tempnames[1][0]);
				for (int i=0; i < caslcodes.size(); i++) {
					for (int j=0; j < caslcodes.get(i).size(); j++) {
						bw.write(caslcodes.get(i).get(j));
					}
				}
				for (String str: constants) {
					bw.write(str);
				}
				//CaslSimulator.appendLibcas(outputFileName);
			}
			bw.close();
			fw.close();
			br.close();
			fr.close();
		} catch (FileNotFoundException e) {
			System.err.println("File not found");
		} catch (IOException e) {
			System.err.println("File not found");
		}
	}

	public String next() {
		errlinenumber = linenumber.pop();
		current++;

		return key.pop();

	}

	public void semerror() {
		if (semerr == false) {
			firstsemerrlinenumber = errlinenumber;
		}
		semerr = true;
	}

	public void synerror() {
		if (synerr == false) {
			firstsynerrlinenumber = errlinenumber;
		}
		synerr = true;
	}

	public void program() {
		caslcode = new LinkedList<String>();
		caslcodes.add(caslcode);

		caslcode.add("CASL\tSTART\tBEGIN\n");
		caslcode.add("BEGIN\tLAD\tGR6, 0\n");
		caslcode.add("\tLAD\tGR7, LIBBUF\n");

		if (! next().equals("SPROGRAM")) {
			synerror(); // まず "program" という文字が出てこなければ構文エラー
		}


		programName(); // 別のEBNFメソッドを呼び出して解析を続ける

		if (! next().equals("SSEMICOLON")) {
			synerror(); // プログラム名の次に ";" が出てこなければ構文エラー
		}

		block();

		complexStatement();

		if (! next().equals("SDOT")) {
			synerror();
		}

		caslcode.add("\tRET\n");

		int i = 0, j = 0;
		while (true) {
			while (true) {
				String name = names[i][j];
				if (name == null) break;
				constants.add(name.toUpperCase() + "\tDS\t1\n");
				j++;
			}
			i++;
			j = 0;
			if (names[i][0] == null) break;
		}

		i = 0; j = 0;
		while(true) {
			while (true) {
				String name = arraynames[i][j];
				if (name == null) break;
				constants.add(name.toUpperCase() + "\tDS\t" + (arraymax[i][j] - arraymin[i][j] + 1) + "\n");
				j++;
			}
			i++;
			j = 0;
			if (arraynames[i][0] == null) break;
		}

		i = 0; j = 0;
		while (i != 100) {
			while (j != 100) {
				String name = tempnames[i][j];
				if (name != null) {
					constants.add(name.toUpperCase() + "\tDS\t1\n");
				}
				j++;
			}
			i++;
			j = 0;
		}

		constants.add("LIBBUF\tDS\t256\n");
		constants.add("\tEND\n");

		{constants.add("; lib.cas\n" +
				";============================================================\n" +
				"; MULT: 掛け算を行うサブルーチン\n" +
				"; GR1 * GR2 -> GR2\n" +
				"MULT\tSTART\n" +
				"\tPUSH\t0,GR1\t; GR1の内容をスタックに退避\n" +
				"\tPUSH\t0,GR3\t; GR3の内容をスタックに退避\n" +
				"\tPUSH\t0,GR4\t; GR4の内容をスタックに退避\n" +
				"\tLAD\tGR3,0\t; GR3を初期化\n" +
				"\tLD\tGR4,GR2\n" +
				"\tJPL\tLOOP\n" +
				"\tXOR\tGR4,=#FFFF\n" +
				"\tADDA\tGR4,=1\n" +
				"LOOP\tSRL\tGR4,1\n" +
				"\tJOV\tONE\n" +
				"\tJUMP\tZERO\n" +
				"ONE\tADDL\tGR3,GR1\n" +
				"ZERO\tSLL\tGR1,1\n" +
				"\tAND\tGR4,GR4\n" +
				"\tJNZ\tLOOP\n" +
				"\tCPA\tGR2,=0\n" +
				"\tJPL\tEND\n" +
				"\tXOR\tGR3,=#FFFF\n" +
				"\tADDA\tGR3,=1\n" +
				"END\tLD\tGR2,GR3\n" +
				"\tPOP\tGR4\n" +
				"\tPOP\tGR3\n" +
				"\tPOP\tGR1\n" +
				"\tRET\n" +
				"\tEND\n" +
				";============================================================\n" +
				"; DIV 割り算を行うサブルーチン\n" +
				"; GR1 / GR2 -> 商は GR2, 余りは GR1\n" +
				"DIV\tSTART\n" +
				"\tPUSH\t0,GR3\n" +
				"\tST\tGR1,A\n" +
				"\tST\tGR2,B\n" +
				"\tCPA\tGR1,=0\n" +
				"\tJPL\tSKIPA\n" +
				"\tXOR\tGR1,=#FFFF\n" +
				"\tADDA\tGR1,=1\n" +
				"SKIPA\tCPA\tGR2,=0\n" +
				"\tJZE\tSKIPD\n" +
				"\tJPL\tSKIPB\n" +
				"\tXOR\tGR2,=#FFFF\n" +
				"\tADDA\tGR2,=1\n" +
				"SKIPB\tLD\tGR3,=0\n" +
				"LOOP\tCPA\tGR1,GR2\n" +
				"\tJMI\tSTEP\n" +
				"\tSUBA\tGR1,GR2\n" +
				"\tLAD\tGR3,1,GR3\n" +
				"\tJUMP\tLOOP\n" +
				"STEP\tLD\tGR2,GR3\n" +
				"\tLD\tGR3,A\n" +
				"\tCPA\tGR3,=0\n" +
				"\tJPL\tSKIPC\n" +
				"\tXOR\tGR1,=#FFFF\n" +
				"\tADDA\tGR1,=1\n" +
				"SKIPC\tXOR\tGR3,B\n" +
				"\tCPA\tGR3,=0\n" +
				"\tJZE\tSKIPD\n" +
				"\tJPL\tSKIPD\n" +
				"\tXOR\tGR2,=#FFFF\n" +
				"\tADDA\tGR2,=1\n" +
				"SKIPD\tPOP\tGR3\n" +
				"\tRET\n" +
				"A\tDS\t1\n" +
				"B\tDS\t1\n" +
				"\tEND\n" +
				";============================================================\n" +
				"; 入力装置から数値データを読み込み，\n" +
				"; その内容をGR2が指すアドレスに格納するサブルーチン\n" +
				"RDINT\tSTART\n" +
				"\tPUSH\t0,GR1\t; GR1の内容をスタックに退避\n" +
				"\tPUSH\t0,GR3\t; GR3の内容をスタックに退避\n" +
				"\tPUSH\t0,GR4\t; GR4の内容をスタックに退避\n" +
				"\tPUSH\t0,GR5\t; GR5の内容をスタックに退避\n" +
				"\tPUSH\t0,GR6\t; GR6の内容をスタックに退避\n" +
				"\tLD\tGR5,GR2\t; GR2が指す番地をGR5にコピー\n" +
				"\tLD\tGR2,=0\t; GR2を初期化\n" +
				"\tLD\tGR3,=0\t; GR3を初期化\n" +
				"\tIN\tINAREA,INLEN\t; 入力を受け取る\n" +
				"\t; 入力がnullかどうかのチェック\n" +
				"\tCPA\tGR3,INLEN\n" +
				"\tJZE\tERROR\n" +
				"\t; 最初の文字が'-'かどうかのチェック\n" +
				"\tLD\tGR4,INAREA,GR3\n" +
				"\tLAD\tGR3,1,GR3\n" +
				"\tLD\tGR6,GR4\t; GR6に入力された先頭の文字を保存\n" +
				"\tCPL\tGR4,=#002D\t; '-'かどうか\n" +
				"\tJZE\tLOOP\n" +
				"\tCPL\tGR4,='0'\t; 数値かどうかのチェック\n" +
				"\tJMI\tERROR\n" +
				"\tCPL\tGR4,='9'\n" +
				"\tJPL\tERROR\n" +
				"\tXOR\tGR4,=#0030\t; 数値だったら変換\n" +
				"\tADDA\tGR2,GR4\n" +
				"\t; 「すでに読み込んだ数値を10倍して，新しく読み込んだ数値と足す」を繰り返す\n" +
				"LOOP\tCPA\tGR3,INLEN\n" +
				"\tJZE\tCODE\t; 入力された文字数とGR3が同じであればループを抜ける\n" +
				"\tLD\tGR1,=10\n" +
				"\tCALL\tMULT\t; GR2の値を10倍する\n" +
				"\tLD\tGR4,INAREA,GR3\n" +
				"\tCPL\tGR4,='0'\t; 数値かどうかのチェック\n" +
				"\tJMI\tERROR\n" +
				"\tCPL\tGR4,='9'\n" +
				"\tJPL\tERROR\n" +
				"\tXOR\tGR4,=#0030\t; GR4の内容を数値に変換\n" +
				"\tADDA\tGR2,GR4\t; GR2にGR1の内容を足す\n" +
				"\tLAD\tGR3,1,GR3\t; GR3(ポインタ)をインクリメント\n" +
				"\tJUMP\tLOOP\n" +
				"\t; 最初の文字が'-'であった場合は-1倍する\n" +
				"CODE\tCPL\tGR6,=#002D\n" +
				"\tJNZ\tEND\n" +
				"\tXOR\tGR2,=#FFFF\n" +
				"\tLAD\tGR2,1,GR2\n" +
				"\tJUMP\tEND\n" +
				"\t; エラーを出力する\n" +
				"ERROR\tOUT\tERRSTR,ERRLEN\n" +
				"END\tST\tGR2,0,GR5\t; GR2の内容をGR5が指す番地に格納する\n" +
				"\tLD\tGR2,GR5\t; GR5が指す番地をGR2に戻す\n" +
				"\tPOP\tGR6\n" +
				"\tPOP\tGR5\n" +
				"\tPOP\tGR4\n" +
				"\tPOP\tGR3\n" +
				"\tPOP\tGR1\n" +
				"\tRET\n" +
				"ERRSTR\tDC\t'illegal input'\n" +
				"ERRLEN\tDC\t13\n" +
				"INAREA\tDS\t6\n" +
				"INLEN\tDS\t1\n" +
				"\tEND\n" +
				";============================================================\n" +
				"; 入力装置から文字を読み込み，\n" +
				"; その内容をGR2が指すアドレスに格納するサブルーチン\n" +
				"RDCH\tSTART\n" +
				"\tIN\tINCHAR,INLEN\n" +
				"\tLD\tGR1,INCHAR\n" +
				"\tST\tGR1,0,GR2\n" +
				"\tRET\n" +
				"INCHAR\tDS\t1\n" +
				"INLEN\tDS\t1\n" +
				"\tEND\n" +
				";============================================================\n" +
				"; 入力装置から，GR1の文字数を読み込む．\n" +
				"; 読み込んだ文字列は，GR2 が指すアドレスから順に格納される\n" +
				"RDSTR\tSTART\n" +
				"\tPUSH\t0,GR3\t; GR3の内容をスタックに退避\n" +
				"\tPUSH\t0,GR4\t; GR4の内容をスタックに退避\n" +
				"\tPUSH\t0,GR5\t; GR5の内容をスタックに退避\n" +
				"\tLAD\tGR4,0\t; GR4を初期化\n" +
				"\tIN\tINSTR,INLEN\n" +
				"LOOP\tCPA\tGR4,GR1\n" +
				"\tJZE\tEND\t; GR1で指定された文字数を超えたら終わり\n" +
				"\tCPA\tGR4,INLEN\n" +
				"\tJZE\tEND\t; 入力された文字数を超えたら終わり\n" +
				"\tLD\tGR5,GR2\n" +
				"\tADDA\tGR5,GR4\t; 文字の格納先番地を計算\n" +
				"\tLD\tGR3,INSTR,GR4\n" +
				"\tST\tGR3,0,GR5\n" +
				"\tLAD\tGR4,1,GR4\n" +
				"\tJUMP\tLOOP\n" +
				"END\tPOP\tGR5\n" +
				"\tPOP\tGR4\n" +
				"\tPOP\tGR3\n" +
				"\tRET\n" +
				"INSTR\tDS\t256\n" +
				"INLEN\tDS\t1\n" +
				"\tEND\n" +
				";============================================================\n" +
				"; 入力装置からの文字列を改行まで読み飛ばすサブルーチン\n" +
				"RDLN\tSTART\n" +
				"\tIN\tINAREA,INLEN\n" +
				"\tRET\n" +
				"INAREA\tDS\t256\n" +
				"INLEN\tDS\t1\n" +
				"\tEND\n" +
				";============================================================\n" +
				"; GR2の内容（数値データ）を出力装置に書き出すサブルーチン\n" +
				"; このサブルーチンが呼ばれたとき，\n" +
				"; GR7には，出力用番地の先頭アドレスが，\n" +
				"; GR6には，現在出力用番地に入っている文字数が，\n" +
				"; それぞれ格納されている．\n" +
				"WRTINT\tSTART\n" +
				"\tPUSH\t0,GR1\t; GR1の内容をスタックに退避\n" +
				"\tPUSH\t0,GR2\t; GR2の内容をスタックに退避\n" +
				"\tPUSH\t0,GR3\t; GR3の内容をスタックに退避\n" +
				"\tPUSH\t0,GR2\t; 数値データをもう一度スタックに退避\n" +
				"\tLD\tGR3,=0\t; GR3はインデックスとして用いる\n" +
				"\t; 数値データが負数である場合は，正の数に変換\n" +
				"\tCPA\tGR2,=0\n" +
				"\tJPL\tLOOP1\n" +
				"\tXOR\tGR2,=#FFFF\n" +
				"\tADDA\tGR2,=1\n" +
				"\t; 数値データを変換しながら，バッファに格納\n" +
				"LOOP1\tLD\tGR1,GR2\n" +
				"\tLD\tGR2,=10\n" +
				"\tCALL\tDIV\n" +
				"\tXOR\tGR1,=#0030\n" +
				"\tST\tGR1,BUFFER,GR3\n" +
				"\tLAD\tGR3,1,GR3\n" +
				"\tCPA\tGR2,=0\n" +
				"\tJNZ\tLOOP1\n" +
				"\t; 数値データが負数であれば，'-'を追加\n" +
				"\tPOP\tGR2\n" +
				"\tCPA\tGR2,=0\n" +
				"\tJZE\tLOOP2\n" +
				"\tJPL\tLOOP2\n" +
				"\tLD\tGR1,='-'\n" +
				"\tST\tGR1,BUFFER,GR3\n" +
				"\tLAD\tGR3,1,GR3\n" +
				"\t; BUFFERを逆順にたどりながら，出力用バッファに格納\n" +
				"LOOP2\tLAD\tGR3,-1,GR3\n" +
				"\tLD\tGR1,BUFFER,GR3\n" +
				"\tLD\tGR2,GR7\n" +
				"\tADDA\tGR2,GR6\n" +
				"\tST\tGR1,0,GR2\n" +
				"\tLAD\tGR6,1,GR6\n" +
				"\tCPA\tGR3,=0\n" +
				"\tJNZ\tLOOP2\n" +
				"END\tPOP\tGR3\n" +
				"\tPOP\tGR2\n" +
				"\tPOP\tGR1\n" +
				"\tRET\n" +
				"BUFFER\tDS\t6\n" +
				"\tEND\n" +
				";============================================================\n" +
				"; GR2の内容（文字）を出力装置に書き出すサブルーチン\n" +
				"; このサブルーチンが呼ばれたとき，\n" +
				"; GR7には，出力用番地の先頭アドレスが，\n" +
				"; GR6には，現在出力用番地に入っている文字数が，\n" +
				"; それぞれ格納されている．\n" +
				"WRTCH\tSTART\n" +
				"\tPUSH\t0,GR1\t; GR1の内容をスタックに退避\n" +
				"\tLD\tGR1,GR7\n" +
				"\tADDA\tGR1,GR6\t; GR1に次の文字を格納する番地を代入\n" +
				"\tST\tGR2,0,GR1\n" +
				"\tLAD\tGR6,1,GR6\n" +
				"\tPOP\tGR1\n" +
				"\tRET\n" +
				"\tEND\n" +
				";============================================================\n" +
				"; GR2の指すメモリ番地から，長さGR1の文字列を出力装置に書き出すサブルーチン\n" +
				"; このサブルーチンが呼ばれたとき，\n" +
				"; GR7には，出力用番地の先頭アドレスが，\n" +
				"; GR6には，現在出力用番地に入っている文字数が，\n" +
				"; それぞれ格納されている．\n" +
				"WRTSTR\tSTART\n" +
				"\tPUSH\t0,GR3\t; GR3の内容をスタックに退避\n" +
				"\tPUSH\t0,GR4\t; GR4の内容をスタックに退避\n" +
				"\tPUSH\t0,GR5\t; GR5の内容をスタックに退避\n" +
				"\tLAD\tGR3,0\t; GR3は制御変数として用いる\n" +
				"LOOP\tCPA\tGR3,GR1\n" +
				"\tJZE\tEND\n" +
				"\tLD\tGR4,GR2\n" +
				"\tADDA\tGR4,GR3\t; 出力する文字の格納番地を計算\n" +
				"\tLD\tGR5,0,GR4\t; 出力する文字をレジスタにコピー\n" +
				"\tLD\tGR4,GR7\n" +
				"\tADDA\tGR4,GR6\t; 出力先の番地を計算\n" +
				"\tST\tGR5,0,GR4\t; 出力装置に書き出し\n" +
				"\tLAD\tGR3,1,GR3\n" +
				"\tLAD\tGR6,1,GR6\n" +
				"\tJUMP\tLOOP\n" +
				"END\tPOP\tGR5\n" +
				"\tPOP\tGR4\n" +
				"\tPOP\tGR3\n" +
				"\tRET\n" +
				"\tEND\n" +
				";============================================================\n" +
				"; 改行を出力装置に書き出すサブルーチン\n" +
				"; 実質的には，GR7で始まるアドレス番地から長さGR6の文字列を出力する\n" +
				"WRTLN\tSTART\n" +
				"\tPUSH\t0,GR1\n" +
				"\tPUSH\t0,GR2\n" +
				"\tPUSH\t0,GR3\n" +
				"\tST\tGR6,OUTLEN\n" +
				"\tLAD\tGR1,0\n" +
				"LOOP\tCPA\tGR1,OUTLEN\n" +
				"\tJZE\tEND\n" +
				"\tLD\tGR2,GR7\n" +
				"\tADDA\tGR2,GR1\n" +
				"\tLD\tGR3,0,GR2\n" +
				"\tST\tGR3,OUTSTR,GR1\n" +
				"\tLAD\tGR1,1,GR1\n" +
				"\tJUMP\tLOOP\n" +
				"END\tOUT\tOUTSTR,OUTLEN\n" +
				"\tLAD\tGR6,0\t; 文字列を出力して，GR6を初期化\n" +
				"\tPOP\tGR3\n" +
				"\tPOP\tGR2\n" +
				"\tPOP\tGR1\n" +
				"\tRET\n" +
				"OUTSTR\tDS\t256\n" +
				"OUTLEN\tDS\t1\n" +
				"\tEND");}
	}

	public void programName() {
		if (! next().equals("SIDENTIFIER")) {
			synerror();
		}
	}

	public void block() {
		variableDeclaration();
		subProgramDeclarationGroup();
		caslcode = caslcodes.get(0);
		proc = 0;
	}

	public void variableDeclaration() {
		String tmp = next();
		if (! tmp.equals("SVAR")) {
			key.push(tmp);
			linenumber.push(errlinenumber);
			current--;
		} else {
			variableDeclarations();
		}
	}

	public void variableDeclarations() {
		variableNames();

		if (! next().equals("SCOLON")) {
			synerror();
		}

		type();

		if (! next().equals("SSEMICOLON")) {
			synerror();
		}

		while (true) {
			String tmp = next();
			if (! tmp.equals("SIDENTIFIER")) {
				key.push(tmp);
				linenumber.push(errlinenumber);
				current--;
				break;
			} else {
				key.push(tmp);
				linenumber.push(errlinenumber);
				current--;

				variableNames();

				if (! next().equals("SCOLON")) {
					synerror();
				}

				type();

				if (! next().equals("SSEMICOLON")) {
					synerror();
				}
			}
		}
	}

	public void variableNames() {
		variableName();

		while (true) {
			String tmp = next();
			if (! tmp.equals("SCOMMA")) {
				key.push(tmp);
				linenumber.push(errlinenumber);
				current--;
				break;
			} else {
				variableName();
			}
		}
	}

	public void variableName() {
		String tmp = raw.get(current+1) + proc;
		if (! next().equals("SIDENTIFIER")) {
			synerror();
		} else if (state == 0) {
			int i = 0;
			while(true) {
				String name = names[proc][i];
				if (name == null) break;
//				System.out.println(name);
				if (name.equals(tmp)) {
					System.out.println("名前の重複エラー");
					semerror();
				}
				i++;
			}
		}
	}

	public void type() {
		String tmp = next();
		if (! tmp.equals("SARRAY")) {
			key.push(tmp);
			linenumber.push(errlinenumber);
			current--;
			standardType();
		} else {
			key.push(tmp);
			linenumber.push(errlinenumber);
			current--;
			arrayType();
		}
	}

	public void standardType() {
		String tmp = next();
		if (! tmp.matches("SINTEGER|SCHAR|SBOOLEAN")) {
			synerror();
		}

		if (subproc) {
			int index = current-2;
			while (raw.get(index-1).equals(",")) {
				index -= 2;
			}
			while (index <= current-2) {
				tempnames[proc][tempvarindex] = raw.get(index) + proc;
				temptype[proc][tempvarindex] = tmp;
				tempvarindex++;
				index += 2;
			}
		} else {
			int index = current-2;
			while (raw.get(index-1).equals(",")) {
				index -= 2;
			}
			while (index <= current-2) {
				names[proc][varindex] = raw.get(index) + proc;
				if (tmp.equals("SBOOLEAN")) {
					attribute[proc][varindex] = 1;
				} else if (tmp.equals("SCHAR")) {
					attribute[proc][varindex] = 3;
				} else if (tmp.equals("SINTEGER")) {
					attribute[proc][varindex] = 4;
				} else {
					attribute[proc][varindex] = 0;
				}
				varindex++;
				index += 2;
			}
		}
	}

	public void arrayType() {
//		buffer4 = new LinkedList<Integer>();
//		buffer5 = new LinkedList<Integer>();
//		buffer6 = new LinkedList<String>();

		arraynames[proc][arrayvarindex] = raw.get(current-1) + Integer.toString(proc);

		if (! next().equals("SARRAY")) {
			synerror();
		}
		if (! next().equals("SLBRACKET")) {
			synerror();
		}
		minimumSubscript();
		arraymin[proc][arrayvarindex] = Integer.parseInt(raw.get(current));
		if (! next().equals("SRANGE")) {
			synerror();
		}
		maximumSubscript();
		if (!Character.isDigit(raw.get(current).charAt(0))) {
			synerror();
		} else {
			arraymax[proc][arrayvarindex] = Integer.parseInt(raw.get(current));
		}

		if (! next().equals("SRBRACKET")) {
			synerror();
		}
		if (! next().equals("SOF")) {
			synerror();
		}

		if (! next().matches("SINTEGER|SCHAR|SBOOLEAN")) {
			synerror();
		}

		arraytype[proc][arrayvarindex] = raw.get(current);
		arrayvarindex++;
	}

	public void minimumSubscript() {
		integral();
	}

	public void maximumSubscript() {
		integral();
	}

	public void integral() {
		String tmp = next();
		if (! tmp.matches("SPLUS|SMINUS")) {
			if (! tmp.equals("SCONSTANT")) {
				synerror();
			}
		} else {
			if (! next().equals("SCONSTANT")) {
				synerror();
			}
		}
	}

	public void sign() {
		if (! next().matches("SPLUS|SMINUS")) {
			synerror();
		}
	}

	public void subProgramDeclarationGroup() {
		while (true) {
			String tmp = next();
			if (! tmp.equals("SPROCEDURE")) {
				key.push(tmp);
				linenumber.push(errlinenumber);
				current--;
				break;
			} else {
				key.push(tmp);
				linenumber.push(errlinenumber);
				current--;
				subProgramDeclaration();
				if (! next().equals("SSEMICOLON")) {
					synerror();
				}
			}
		}
	}

	public void subProgramDeclaration() {
		caslcode = new LinkedList<String>();
		caslcodes.add(caslcode);
		state = 1;
		caslcode.add("PROC" + proc + "\tNOP\n");
		proc++;
		varindex = 0;
		tempvarindex = 0;
		subProgramHead();
		variableDeclaration();
		complexStatement();
		caslcode.add("\tRET\n");
		state = 0;
	}

	public void subProgramHead() {
		if (! next().equals("SPROCEDURE")) {
			synerror();
		}

		subprogramnames.add(raw.get(current+1));

		procedureName();

		temporaryParameter();

		if (! next().equals("SSEMICOLON")) {
			synerror();
		}
	}

	public void procedureName() {
		boolean ok = false;
		int i = 0;
		for (String subprogramname: subprogramnames) {
			if (raw.get(current+1).equals(subprogramname)) {
				ok = true;
			}
			i++;
		}
		if (!ok) {
			semerror();
		}

		if (! next().equals("SIDENTIFIER")) {
			synerror();
		}
	}

	public void temporaryParameter() {
		String tmp = next();
		if (! tmp.equals("SLPAREN")) {
			key.push(tmp);
			linenumber.push(errlinenumber);
			current--;
		} else {
			temporaryParameters();
			if (! next().equals("SRPAREN")) {
				synerror();
			}
		}
	}

	public void temporaryParameters() {
		temporaryParameterNames();

		if (! next().equals("SCOLON")) {
			synerror();
		}

		subproc = true;
		standardType();
		subproc = false;

		while (true) {
			String tmp = next();
			if (! tmp.equals("SSEMICOLON")) {
				key.push(tmp);
				linenumber.push(errlinenumber);
				current--;
				break;
			} else {
				temporaryParameterNames();

				if (! next().equals("SCOLON")) {
					synerror();
				}

				subproc = true;
				standardType();
				subproc = false;
			}
		}
	}

	public void temporaryParameterNames() {
		temporaryParameterName();

		while (true) {
			String tmp = next();
			if (! tmp.equals("SCOMMA")) {
				key.push(tmp);
				linenumber.push(errlinenumber);
				current--;
				break;
			} else {
				temporaryParameterName();
			}
		}
	}

	public void temporaryParameterName() {
		if (! next().equals("SIDENTIFIER")) {
			synerror();
		}
	}

	public void complexStatement() {
		if (! next().equals("SBEGIN")) {
			synerror();
		}

		statements();

		if (! next().equals("SEND")) {
			synerror();
		}
	}

	public void statements() {
		statement();

		while (true) {
			String tmp = next();
			if (! tmp.equals("SSEMICOLON")) {
				key.push(tmp);
				linenumber.push(errlinenumber);
				current--;
				break;
			} else {
				statement();
			}
		}
	}

	public void statement() {
		String tmp = next();
		if (tmp.equals("SIF")) {
			int subloop = loop++;

			int i = 0;
			while(true) {
				String name = names[proc][i];
				if (name == null) break;
				if (name.equals(raw.get(current+1) + proc)) {
					if (attribute[proc][i] != 1 && !raw.get(current+2).matches("=|<>|<|<=|>|>=|\\+|-|or|\\*|/|div|mod|and|\\[")) {
						semerror();
					}
				}
				i++;
			}

			formula();

			caslcode.add("\tPOP\tGR1\n");
			caslcode.add("\tCPA\tGR1, =1\n");
			caslcode.add("\tJNZ\tELSE" + subloop + "\n");

			if (! next().equals("STHEN")) {
				synerror();
			}
			complexStatement();

			caslcode.add("\tJUMP\tENDIF" + subloop + "\n");
			caslcode.add("ELSE" + subloop + "\tNOP\n");

			tmp = next();
			if (! tmp.equals("SELSE")) {
				key.push(tmp);
				linenumber.push(errlinenumber);
				current--;
			} else {
				complexStatement();
			}
			caslcode.add("ENDIF" + subloop + "\tNOP\n");
		} else if (tmp.equals("SWHILE")) {
			int subloop = loop++;
			caslcode.add("LOOP" + subloop + "\tNOP\n");
			formula();
			caslcode.add("\tPOP\tGR1\n");
			caslcode.add("\tCPA\tGR1, =1\n");
			caslcode.add("\tJNZ\tENDLP" + subloop + "\n");
			if (! next().equals("SDO")) {
				synerror();
			}
			statement();

			caslcode.add("\tJUMP\tLOOP" + subloop + "\n");
			caslcode.add("ENDLP" + subloop + "\tNOP\n");
		} else {
			key.push(tmp);
			linenumber.push(errlinenumber);
			current--;
			basicStatement();
		}
	}

	public void basicStatement() {
		String tmp1 = next();
		int tmp2 = errlinenumber;
		if (tmp1.equals("SIDENTIFIER")) {
			String tmp3 = next();
			if(tmp3.matches("SASSIGN|SLBRACKET")) {
				key.push(tmp3);
				linenumber.push(errlinenumber);
				current--;
				key.push(tmp1);
				linenumber.push(tmp2);
				current--;
				assignStatement();
			} else if(tmp3.matches("SLPAREN|SSEMICOLON|SEND")) {
				key.push(tmp3);
				linenumber.push(errlinenumber);
				current--;
				key.push(tmp1);
				linenumber.push(tmp2);
				current--;
				procedureCallingStatement();
			} else {
				synerror();
			}
		} else if (tmp1.matches("SREADLN|SWRITELN")) {
			key.push(tmp1);
			linenumber.push(tmp2);
			current--;
			ioStatement();
		} else if (tmp1.equals("SBEGIN")) {
			key.push(tmp1);
			linenumber.push(tmp2);
			current--;
			complexStatement();
		} else if (tmp1.equals("SEND")) {
			key.push(tmp1);
			linenumber.push(tmp2);
			current--;
		} else {
			synerror();
		}
	}

	public void assignStatement() {
		String var = raw.get(current+1);

		leftSide();

		if (! next().equals("SASSIGN")) {
			synerror();
		}

		formula();

		int i = proc;
		int j = 0;
		boolean ok = false;
		while (i >= 0 && !ok) {
			while (true) {
				String name = names[i][j];
				if (name == null) break;
				String str = var + i;
				if (name.equals(str)) {
					var = str;
					ok = true;
				}
				j++;
			}
			j = 0;
			i--;
		}

		i = proc; j = 0; int min = 0;
		ok = false;
		boolean isarray = false;
		while(i >= 0 && !ok) {
			while (true) {
				String name = arraynames[i][j];
				if (name == null) break;
				String str = var + i;
				if (str.equals(name)) {
					min = arraymin[i][j];
					ok = true;
					var = str;
					isarray = true;
				}
				j++;
			}
			j = 0;
			i--;
		}

		if (isarray) {
			caslcode.add("\tPOP\tGR2\n");
			caslcode.add("\tPOP\tGR1\n");
			caslcode.add("\tSUBA\tGR1, =" + min + "\n");
			caslcode.add("\tST\tGR2, " + var.toUpperCase() + ", GR1\n");
		} else {
			caslcode.add("\tPOP\tGR1\n");
			caslcode.add("\tST\tGR1, " + var.toUpperCase() + "\n");
		}
	}

	public void leftSide() {
		boolean ok = false;
		int i = 0;
		while(true) {
			String name = names[proc][i];
			if (name == null) break;
			String str = raw.get(current+1) + proc;
			if (str.equals(name)) {
				ok = true;
			}
			i++;
		}

		i = 0;
		while(true) {
			String name = arraynames[proc][i];
			if (name == null) break;
			String str = raw.get(current+1) + proc;
			if (str.equals(name)) {
				ok = true;
				if (!raw.get(current+2).equals("[")) {
					semerror();
				}
			}
			i++;
		}

		if ((ok == false) && (state == 0)) {
			System.out.println("宣言されていない変数");
			semerror();
		}

		i = 0;

		while(true) {
			String name = names[proc][i];
			if (name == null) break;
			if ((raw.get(current+1)+proc).equals(name)) {
				int j = 0;
				while(true) {
					String name2 = names[proc][j];
					if (name2 == null) break;
					if ((raw.get(current+3)+proc).equals(name2)) {
						if ((attribute[proc][i] != attribute[proc][j]) && (attribute[proc][i] != 1)) {
							semerror();
						}
					}
					j++;
				}
			}
			i++;
		}

		left = true;
		variable();
		left = false;
	}

	public void variable() {
		if (! next().equals("SIDENTIFIER")) {
			synerror();
		}
		String tmp = next();
		if (! tmp.equals("SLBRACKET")) {
			key.push(tmp);
			linenumber.push(errlinenumber);
			current--;

			if (!left) {
				//右辺かつ次の文字が[ではない

				int i = proc;
				int j = 0;
				boolean ok = false;
				while (i >= 0 && !ok) {
					while (true) {
						String name = tempnames[i][j];
						if (name == null) break;
						String str = raw.get(current) + i;
						if (name.equals(str)) {
							caslcode.add("\tLD\tGR1, " + str.toUpperCase() + "\n");
							caslcode.add("\tPUSH\t0, GR1\n");
							ok = true;
						}
						j++;
					}
					j = 0;
					i--;
				}

				i = proc;
				j = 0;
				while (i >= 0 && !ok) {
					while (true) {
						String name = names[i][j];
						if (name == null) break;
						String str = raw.get(current) + i;
						if (name.equals(str)) {
							caslcode.add("\tLD\tGR1, " + str.toUpperCase() + "\n");
							caslcode.add("\tPUSH\t0, GR1\n");
							ok = true;
						}
						j++;
					}
					j = 0;
					i--;
				}
			} else {
				//左辺かつ次の文字が[ではない
				if (insubscript) {
					int i = proc;
					int j = 0;
					boolean ok = false;
					while (i >= 0 && !ok) {
						while (true) {
							String name = names[i][j];
							if (name == null) break;
							String str = raw.get(current) + i;
							if (name.equals(str)) {
								caslcode.add("\tLD\tGR1, " + str.toUpperCase() + "\n");
								caslcode.add("\tPUSH\t0, GR1\n");
								ok = true;
							}
							j++;
						}
						j = 0;
						i--;
					}
				}
			}
		} else {
			if(!left) {
				//右辺かつ次の文字が[

				String var = raw.get(current - 1);
				int i = proc, j = 0, min = 0;
				boolean ok = false;
				while(i >= 0 && !ok) {
					while (true) {
						String name = arraynames[i][j];
						if (name == null) break;
						String str = var + i;
						if (str.equals(name)) {
							min = arraymin[i][j];
							ok = true;
							var = str;
						}
						j++;
					}
					j = 0;
					i--;
				}

				state = 2;
				subscript();

				caslcode.add("\tPOP\tGR2\n");
				caslcode.add("\tSUBA\tGR2, =" + min + "\n");
				caslcode.add("\tLD\tGR1, " + var.toUpperCase() + ", GR2\n");
				caslcode.add("\tPUSH\t0, GR1\n");
			} else {
				//左辺かつ次の文字が[

				int i = proc;
				int j = 0;
				boolean ok = false;
				while (i >= 0 && !ok) {
					while (true) {
						String name = names[i][j];
						if (name == null) break;
						String str = raw.get(current+1) + i;
						if (name.equals(str)) {
							ok = true;
						}
						j++;
					}
					j = 0;
					i--;
				}

				String var = raw.get(current+1);
				i = proc; j = 0;
				int min = 0;
				ok = false;
				while(i >= 0 && !ok) {
					while (true) {
						String name = arraynames[i][j];
						if (name == null) break;
						String str = var + i;
						if (str.equals(name)) {
							min = arraymin[i][j];
							ok = true;
							var = str;
						}
						j++;
					}
					j = 0;
					i--;
				}

				insubscript = true;
				subscript();
				insubscript = false;

				if (ok) {
					caslcode.add("\tPOP\tGR2\n");
					caslcode.add("\tSUBA\tGR2, =" + min + "\n");
					caslcode.add("\tLD\tGR1, " + var.toUpperCase() + ", GR2\n");
					caslcode.add("\tPUSH\t0, GR1\n");
				}
			}

			if (! next().equals("SRBRACKET")) {
				synerror();
			}
		}
	}

	public void subscript() {
		formula();
	}

	public void procedureCallingStatement() {
		int procnum = -1;

		int i = 0;
		for (String str : subprogramnames) {
			if (str.equals(raw.get(current+1))) {
				procnum = i;
			}
			i++;
		}

		procedureName();

		String tmp = next();
		if (! tmp.equals("SLPAREN")) {
			key.push(tmp);
			linenumber.push(errlinenumber);
			current--;
		} else {
			formulas();
			if (! next().equals("SRPAREN")) {
				synerror();
			}
		}

		i = 99;
		while(i >= 0) {
			String name = tempnames[procnum+1][i];
			if (name != null) {
				caslcode.add("\tPOP\tGR1\n");
				caslcode.add("\tST\tGR1, " + name.toUpperCase() + "\n");
			}
			i--;
		}

		caslcode.add("\tCALL\tPROC" + procnum + "\n");
	}

	public void formulas() {
		String strattribute = formula();

		if (wrtstr) {
			if (strattribute.equals("SCHAR")) {
				caslcode.add("\tPOP\tGR2\n");
				caslcode.add("\tCALL\tWRTCH\n");
			} else if (strattribute.equals("SINTEGER")) {
				caslcode.add("\tPOP\tGR2\n");
				caslcode.add("\tCALL\tWRTINT\n");
			} else if (strattribute.equals("SSTRING")) {
				caslcode.add("\tPOP\tGR1\n");
				caslcode.add("\tLAD\tGR2, CHAR" + charnum2 + "\n");
				caslcode.add("\tCALL\tWRTSTR\n");
				charnum2++;
			}
		}

		while (true) {
			String tmp = next();
			if (! tmp.equals("SCOMMA")) {
				key.push(tmp);
				linenumber.push(errlinenumber);
				current--;
				break;
			} else {
				strattribute = formula();

				if (wrtstr) {
					if (strattribute.equals("SCHAR")) {
						caslcode.add("\tPOP\tGR2\n");
						caslcode.add("\tCALL\tWRTCH\n");
					} else if (strattribute.equals("SINTEGER")) {
						caslcode.add("\tPOP\tGR2\n");
						caslcode.add("\tCALL\tWRTINT\n");
					} else if (strattribute.equals("SSTRING")) {
						caslcode.add("\tPOP\tGR1\n");
						caslcode.add("\tLAD\tGR2, CHAR" + charnum2 + "\n");
						caslcode.add("\tCALL\tWRTSTR\n");
						charnum2++;
					}
				}
			}
		}

		if (wrtstr) {
			caslcode.add("\tCALL\tWRTLN\n");
		}
	}

	public String formula() {
		String strattribute = simpleFormula();

		if (insubscript) {
			if (strattribute.equals("SBOOLEAN")) {
				semerror();
			}
		}

		String tmp = next();
		if (tmp.equals("SEQUAL")) {
			simpleFormula();
			fork++;
			caslcode.add("\tPOP\tGR2\n");
			caslcode.add("\tPOP\tGR1\n");
			caslcode.add("\tCPA\tGR1, GR2\n");
			caslcode.add("\tJZE\tTRUE" + fork +"\n");
			caslcode.add("\tPUSH\t0\n");
			caslcode.add("\tJUMP\tBOTH" + fork + "\n");
			caslcode.add("TRUE" + fork + "\tPUSH\t1\n");
			caslcode.add("BOTH" + fork + "\tNOP\n");
			return "SBOOLEAN";
		} else if (tmp.equals("SNOTEQUAL")) {
			simpleFormula();
			fork++;
			caslcode.add("\tPOP\tGR2\n");
			caslcode.add("\tPOP\tGR1\n");
			caslcode.add("\tCPA\tGR1, GR2\n");
			caslcode.add("\tJNZ\tTRUE" + fork + "\n");
			caslcode.add("\tPUSH\t0\n");
			caslcode.add("\tJUMP\tBOTH" + fork + "\n");
			caslcode.add("TRUE" + fork + "\tPUSH\t1\n");
			caslcode.add("BOTH" + fork + "\tNOP\n");
			return "SBOOLEAN";
		} else if (tmp.equals("SGREATEQUAL")) {
			simpleFormula();
			fork++;
			caslcode.add("\tPOP\tGR2\n");
			caslcode.add("\tPOP\tGR1\n");
			caslcode.add("\tCPA\tGR1, GR2\n");
			caslcode.add("\tJZE\tTRUE" + fork +"\n");
			caslcode.add("\tJPL\tTRUE" + fork +"\n");
			caslcode.add("\tPUSH\t0\n");
			caslcode.add("\tJUMP\tBOTH" + fork + "\n");
			caslcode.add("TRUE" + fork + "\tPUSH\t1\n");
			caslcode.add("BOTH" + fork + "\tNOP\n");
			return "SBOOLEAN";
		} else if (tmp.equals("SLESSEQUAL")) {
			simpleFormula();
			fork++;
			caslcode.add("\tPOP\tGR2\n");
			caslcode.add("\tPOP\tGR1\n");
			caslcode.add("\tCPA\tGR1, GR2\n");
			caslcode.add("\tJZE\tTRUE" + fork +"\n");
			caslcode.add("\tJMI\tTRUE" + fork +"\n");
			caslcode.add("\tPUSH\t0\n");
			caslcode.add("\tJUMP\tBOTH" + fork + "\n");
			caslcode.add("TRUE" + fork + "\tPUSH\t1\n");
			caslcode.add("BOTH" + fork + "\tNOP\n");
			return "SBOOLEAN";
		} else if (tmp.equals("SGREAT")) {
			simpleFormula();
			fork++;
			caslcode.add("\tPOP\tGR2\n");
			caslcode.add("\tPOP\tGR1\n");
			caslcode.add("\tCPA\tGR1, GR2\n");
			caslcode.add("\tJPL\tTRUE" + fork +"\n");
			caslcode.add("\tPUSH\t0\n");
			caslcode.add("\tJUMP\tBOTH" + fork + "\n");
			caslcode.add("TRUE" + fork + "\tPUSH\t1\n");
			caslcode.add("BOTH" + fork + "\tNOP\n");
			return "SBOOLEAN";
		} else if (tmp.equals("SLESS")) {
			simpleFormula();
			fork++;
			caslcode.add("\tPOP\tGR2\n");
			caslcode.add("\tPOP\tGR1\n");
			caslcode.add("\tCPA\tGR1, GR2\n");
			caslcode.add("\tJMI\tTRUE" + fork +"\n");
			caslcode.add("\tPUSH\t0\n");
			caslcode.add("\tJUMP\tBOTH" + fork + "\n");
			caslcode.add("TRUE" + fork + "\tPUSH\t1\n");
			caslcode.add("BOTH" + fork + "\tNOP\n");
			return "SBOOLEAN";
		} else {
			key.push(tmp);
			linenumber.push(errlinenumber);
			current--;
			return strattribute;
		}
	}

	public String simpleFormula() {
		String strattribute = "";
		String tmp = next();
		if (! tmp.matches("SPLUS|SMINUS")) {
			key.push(tmp);
			linenumber.push(errlinenumber);
			current--;
			strattribute = term();
		} else if (tmp.equals("SPLUS")) {
			strattribute = term();
		} else if (tmp.equals("SMINUS")) {
			strattribute = term();
			caslcode.add("\tPOP\tGR1\n");
			caslcode.add("\tLD\tGR2, =-1\n");
			caslcode.add("\tCALL\tMULT\n");
			caslcode.add("\tPUSH\t0, GR2\n");
		}
		while (true) {
			tmp = next();
			if (! tmp.matches("SPLUS|SMINUS|SOR")) {
				key.push(tmp);
				linenumber.push(errlinenumber);
				current--;

				break;
			} else {
				if (raw.get(current+1).charAt(0) == '\'') {
					semerror();
				}
				strattribute = term();

				if (tmp.equals("SPLUS")) {
					caslcode.add("\tPOP\tGR2\n");
					caslcode.add("\tPOP\tGR1\n");
					caslcode.add("\tADDA\tGR1, GR2\n");
					caslcode.add("\tPUSH\t0, GR1\n");
				} else if (tmp.equals("SMINUS")) {
					caslcode.add("\tPOP\tGR2\n");
					caslcode.add("\tPOP\tGR1\n");
					caslcode.add("\tSUBA\tGR1, GR2\n");
					caslcode.add("\tPUSH\t0, GR1\n");
				} else if (tmp.equals("SOR")) {
					caslcode.add("\tPOP\tGR2\n");
					caslcode.add("\tPOP\tGR1\n");
					caslcode.add("\tSUBA\tGR1, GR2\n");
					caslcode.add("\tPUSH\t0, GR1\n");
				}
			}
		}

		return strattribute;
	}

	public String term() {
		String strattribute = factor();
		while (true) {
			String tmp = next();

			if (tmp.equals("SSTAR")) {
				strattribute = factor();
				caslcode.add("\tPOP\tGR2\n");
				caslcode.add("\tPOP\tGR1\n");
				caslcode.add("\tCALL\tMULT\n");
				caslcode.add("\tPUSH\t0, GR2\n");
			} else if (tmp.equals("SDIVD")) {
				strattribute = factor();
				caslcode.add("\tPOP\tGR2\n");
				caslcode.add("\tPOP\tGR1\n");
				caslcode.add("\tCALL\tDIV\n");
				caslcode.add("\tPUSH\t0, GR2\n");
			} else if (tmp.equals("SMOD")) {
				strattribute = factor();
				caslcode.add("\tPOP\tGR2\n");
				caslcode.add("\tPOP\tGR1\n");
				caslcode.add("\tCALL\tDIV\n");
				caslcode.add("\tPUSH\t0, GR1\n");
			} else if (tmp.equals("SAND")) {
				strattribute = factor();
				caslcode.add("\tPOP\tGR2\n");
				caslcode.add("\tPOP\tGR1\n");
				caslcode.add("\tAND\tGR1, GR2\n");
				caslcode.add("\tPUSH\t0, GR1\n");
			} else {
				key.push(tmp);
				linenumber.push(errlinenumber);
				current--;
				break;
			}
		}
		return strattribute;
	}

	public String factor() {
		String strattribute = "";
		String tmp = next();
		if (tmp.equals("SIDENTIFIER")) {
			key.push(tmp);
			linenumber.push(errlinenumber);
			current--;

			int i = proc;
			int j = 0;
			boolean ok = false;
			while(i >= 0 && !ok) {
				while (true) {
					String name = names[i][j];
					if (name == null) break;
					String str = raw.get(current + 1) + i;
					if (name.equals(str)) {
						ok = true;
						if (attribute[i][j] == 1) {
							strattribute = "SBOOLEAN";
						} else if (attribute[i][j] == 3) {
							strattribute = "SCHAR";
						} else if (attribute[i][j] == 4) {
							strattribute = "SINTEGER";
						}
					}
					j++;
				}
				j = 0;
				i--;
			}

			i = proc;
			j = 0;
			ok = false;
			while(i >= 0 && !ok) {
				while (true) {
					String name = arraynames[i][j];
					if (name == null) break;
					String str = raw.get(current + 1) + i;
					if (name.equals(str)) {
						ok = true;
						if (arraytype[i][j].equals("boolean")) {
							strattribute = "SBOOLEAN";
						} else if (arraytype[i][j].equals("char")) {
							strattribute = "SCHAR";
						} else if (arraytype[i][j].equals("integer")) {
							strattribute = "SINTEGER";
						}
					}
					j++;
				}
				j = 0;
				i--;
			}

			i = 0;
			while(true) {
				String name = tempnames[proc][i];
				if (name == null) break;
				String str = raw.get(current+1) + proc;
				if (name.equals(str)) {
					strattribute = temptype[proc][i];
				}
				i++;
			}

			variable();
			return strattribute;
		} else if (tmp.equals("SLPAREN")) {
			strattribute = formula();
			if (! next().equals("SRPAREN")) {
				synerror();
			}
			return strattribute;
		} else if (tmp.equals("SNOT")) {
			factor();
			caslcode.add("\tPOP\tGR1\n");
			caslcode.add("\tXOR\tGR1, =1\n");
			caslcode.add("\tPUSH\t0, GR1\n");
			return "SBOOLEAN";
		} else {
			key.push(tmp);
			linenumber.push(errlinenumber);
			current--;
			return constant();
		}
	}

	public void ioStatement() {
		String tmp = next();
		if (tmp.equals("SREADLN")) {
			tmp = next();
			if (! tmp.equals("SLPAREN")) {
				key.push(tmp);
				linenumber.push(errlinenumber);
				current--;
			} else {
				variables();
				tmp = next();
				if (! tmp.equals("SRPAREN")) {
					synerror();
				}
			}
		} else if (tmp.equals("SWRITELN")) {
			tmp = next();
			if (! tmp.equals("SLPAREN")) {
				key.push(tmp);
				linenumber.push(errlinenumber);
				current--;
			} else {
				wrtstr = true;
				formulas();
				wrtstr = false;
				tmp = next();
				if (! tmp.equals("SRPAREN")) {
					synerror();
				}
			}
		} else {
			synerror();
		}
	}

	public void variables() {
		variable();
		while (true) {
			String tmp = next();
			if (! tmp.equals("SCOMMA")) {
				key.push(tmp);
				linenumber.push(errlinenumber);
				current--;
				break;
			} else {
				variable();
			}
		}
	}

	public String constant() {
		String strattribute = "";
		String tmp = next();
		if (!tmp.matches("SCONSTANT|SSTRING|SFALSE|STRUE")) {
			synerror();
		}

		if (tmp.equals("SCONSTANT")) {
			key.push(tmp);
			linenumber.push(errlinenumber);
			current--;
			if (! next().equals("SCONSTANT")) {
				synerror();
			}
			caslcode.add("\tPUSH\t" + raw.get(current) + "\n");
			strattribute = "SINTEGER";
		}

		if (tmp.equals("SSTRING")) {
			key.push(tmp);
			linenumber.push(errlinenumber);
			current--;
			if (! next().equals("SSTRING")) {
				synerror();
			}
			if (raw.get(current).length() > 3) {
				caslcode.add("\tPUSH\t" + Integer.toString(raw.get(current).length()-2) + "\n");
				constants.add("CHAR" + charnum + "\tDC\t" + raw.get(current) + "\n");
				charnum++;
				strattribute = "SSTRING";
			} else {
				caslcode.add("\tLD\tGR1, =" + raw.get(current) + "\n");
				caslcode.add("\tPUSH\t0, GR1\n");
				strattribute = "SCHAR";
			}
		}

		if (tmp.equals("SFALSE")) {
			caslcode.add("\tPUSH\t0\n");
			strattribute = "SBOOLEAN";
		}

		if (tmp.equals("STRUE")) {
			caslcode.add("\tPUSH\t1\n");
			strattribute = "SBOOLEAN";
		}

		return strattribute;
	}
}
