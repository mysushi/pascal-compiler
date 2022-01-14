package enshud.s1.lexer;

import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class Lexer {

	static final String[] ctable = new String[]{
			"SNULL", "SNULL", "SNULL", "SNULL", "SNULL", "SNULL", "SNULL", "SNULL",
			"SNULL", "SNULL", "SNULL", "SNULL", "SNULL", "SNULL", "SNULL", "SNULL",
			"SNULL", "SNULL", "SNULL", "SNULL", "SNULL", "SNULL", "SNULL", "SNULL",
			"SNULL", "SNULL", "SNULL", "SNULL", "SNULL", "SNULL", "SNULL", "SNULL",
			"SNULL", "SLNOT", "SDQUOTE", "SNULL", "SNULL", "SMOD", "SBAND", "SSQUOTE",
			"SLPAREN", "SRPAREN", "SSTAR", "SPLUS", "SCOMMA", "SMINUS", "SDOT", "SDIV",
			"SDIGIT", "SDIGIT", "SDIGIT", "SDIGIT", "SDIGIT", "SDIGIT", "SDIGIT", "SDIGIT",
			"SDIGIT", "SDIGIT", "SCOLON", "SSEMI", "SLESS", "SASSIGN", "SGREAT", "SQUEST",
			"SNULL", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA",
			"SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA",
			"SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA",
			"SALPHA", "SALPHA", "SALPHA", "SLSQP", "SNULL", "SRSQP", "SBEOR", "SALPHA",
			"SNULL", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA",
			"SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA",
			"SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA",
			"SALPHA", "SALPHA", "SALPHA", "SLBRACE", "SBOR", "SRBRACE", "SBNOT", "SNULL",
			"SNULL", "SNULL", "SNULL", "SNULL", "SNULL", "SNULL", "SNULL", "SNULL",
			"SNULL", "SNULL", "SNULL", "SNULL", "SNULL", "SNULL", "SNULL", "SNULL",
			"SNULL", "SNULL", "SNULL", "SNULL", "SNULL", "SNULL", "SNULL", "SNULL",
			"SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA",
			"SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA",
			"SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA",
			"SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA",
			"SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA",
			"SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA",
			"SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA",
			"SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA", "SALPHA",
			"SNULL", "SNULL", "SNULL", "SNULL", "SNULL", "SNULL", "SNULL", "SNULL",
			"SNULL", "SNULL", "SNULL", "SNULL", "SNULL", "SNULL", "SNULL", "SNULL",
			"SNULL", "SNULL", "SNULL", "SNULL", "SNULL", "SNULL", "SNULL", "SNULL",
			"SNULL", "SNULL", "SNULL", "SNULL", "SNULL", "SNULL", "SNULL", "SNULL",
			"SEOF"
	};

	static final Map<String, String> key = new HashMap<String, String>() {
		{
			put("and", "\tSAND\t0\t");
			put("array", "\tSARRAY\t1\t");
			put("begin", "\tSBEGIN\t2\t");
			put("boolean", "\tSBOOLEAN\t3\t");
			put("char", "\tSCHAR\t4\t");
			put("div", "\tSDIVD\t5\t");
			put("/", "\tSDIVD\t5\t");
			put("do", "\tSDO\t6\t");
			put("else", "\tSELSE\t7\t");
			put("end", "\tSEND\t8\t");
			put("false", "\tSFALSE\t9\t");
			put("if", "\tSIF\t10\t");
			put("integer", "\tSINTEGER\t11\t");
			put("mod", "\tSMOD\t12\t");
			put("not", "\tSNOT\t13\t");
			put("of", "\tSOF\t14\t");
			put("or", "\tSOR\t15\t");
			put("procedure", "\tSPROCEDURE\t16\t");
			put("program", "\tSPROGRAM\t17\t");
			put("readln", "\tREADLN\t18\t");
			put("then", "\tSTHEN\t19\t");
			put("true", "\tSTRUE\t20\t");
			put("var", "\tSVAR\t21\t");
			put("while", "\tSWHILE\t22\t");
			put("writeln", "\tSWRITELN\t23\t");
			put("=", "\tSEQUAL\t24\t");
			put("<>", "\tSNOTEQUAL\t25\t");
			put("<", "\tSLESS\t26\t");
			put("<=", "\tSLESSEQUAL\t27\t");
			put(">=", "\tSGREATEQUAL\t28\t");
			put(">", "\tSGREAT\t29\t");
			put("+", "\tSPLUS\t30\t");
			put("-", "\tSMINUS\t31\t");
			put("*", "\tSSTAR\t32\t");
			put("(", "\tSLPAREN\t33\t");
			put(")", "\tSRPAREN\t34\t");
			put("[", "\tSLBRACKET\t35\t");
			put("]", "\tSRBRACKET\t36\t");
			put(";", "\tSSEMICOLON\t37\t");
			put(":", "\tSCOLON\t38\t");
			put("..", "\tSRANGE\t39\t");
			put(":=", "\tSASSIGN\t40\t");
			put(",", "\tSCOMMA\t41\t");
			put(".", "\tSDOT\t42\t");
		}
	};



	/**
	 * サンプルmainメソッド．
	 * 単体テストの対象ではないので自由に改変しても良い．
	 */
	public static void main(final String[] args) {
		// normalの確認
		new Lexer().run("data/pas/normal01.pas", "tmp/out1.ts");
		new Lexer().run("data/pas/normal02.pas", "tmp/out2.ts");
		new Lexer().run("data/pas/normal03.pas", "tmp/out3.ts");
	}

	/**
	 * TODO
	 *
	 * 開発対象となるLexer実行メソッド．
	 * 以下の仕様を満たすこと．
	 *
	 * 仕様:
	 * 第一引数で指定されたpasファイルを読み込み，トークン列に分割する．
	 * トークン列は第二引数で指定されたtsファイルに書き出すこと．
	 * 正常に処理が終了した場合は標準出力に"OK"を，
	 * 入力ファイルが見つからない場合は標準エラーに"File not found"と出力して終了すること．
	 *
	 * @param inputFileName 入力pasファイル名
	 * @param outputFileName 出力tsファイル名
	 */
	public void run(final String inputFileName, final String outputFileName) {

		FileReader fr = null;
		BufferedReader br = null;

		File file = new File(inputFileName);

		try {
			fr = new FileReader(inputFileName);
			br = new BufferedReader(fr);

			File outputFile = new File(outputFileName);
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));


			String str;
			int linenumber = 0;
			int flag = 3;

			while ((str = br.readLine()) != null) {
				str += " ";
				linenumber++;
				for (int i = 0; i < str.length(); i++) {
					char c = str.charAt(i);
					String token = "";
					if (ctable[c].equals("SALPHA")) {
						flag = 0;
						do {
							token += String.valueOf(c);
							c = str.charAt(++i);
						} while (ctable[c].equals("SALPHA") || ctable[c].equals("SDIGIT"));
						i--;
					} else if (ctable[c].equals("SDIGIT")) {
						flag = 1;
						do {
							token += String.valueOf(c);
							c = str.charAt(++i);
						} while (ctable[c].equals("SDIGIT"));
						i--;
					} else if (ctable[c].equals("SSQUOTE")) {
						flag = 2;
						do {
							token += String.valueOf(c);
							c = str.charAt(++i);
						} while (!ctable[c].equals("SSQUOTE"));
						token += "'";
					} else if (ctable[c].equals("SLESS")) {
						if (str.charAt(++i) == '>') {
							token = "<>";
						} else if (str.charAt(i) == '=') {
							token = "<=";
						} else {
							token = "<";
							i--;
						}
					} else if (ctable[c].equals("SGREAT")) {
						if (str.charAt(++i) == '=') {
							token = ">=";
						} else {
							token = ">";
							i--;
						}
					} else if (ctable[c].equals("SDOT")) {
						if (str.charAt(++i) == '.') {
							token = "..";
						} else {
							token = ".";
							i--;
						}
					} else if (ctable[c].equals("SCOLON")) {
						if (str.charAt(++i) == '=') {
							token = ":=";
						} else {
							token = ":";
							i--;
						}
					} else if (ctable[c].equals("SASSIGN") || ctable[c].equals("SPLUS") || ctable[c].equals("SMINUS") || ctable[c].equals("SSTAR") || ctable[c].equals("SDIV") || ctable[c].equals("SLPAREN") || ctable[c].equals("SRPAREN") || ctable[c].equals("SLSQP") || ctable[c].equals("SRSQP") || ctable[c].equals("SSEMI") || ctable[c].equals("SCOMMA")) {
						token = String.valueOf(c);
					} else if (ctable[c].equals("SNULL")) {
						continue;
					} else if (ctable[c].equals("SLBRACE")) {
						do {
							c = str.charAt(++i);
						} while (!ctable[c].equals("SRBRACE"));
						continue;
					}

					String out = "";

					if (key.containsKey(token)) {
						out = token + key.get(token) + linenumber;
					} else if (flag == 0) {
						out = token + "\tSIDENTIFIER\t43\t" + linenumber;
					} else if (flag == 1) {
						out = token + "\tSCONSTANT\t44\t" + linenumber;
					} else if (flag == 2) {
						out = token + "\tSSTRING\t45\t" + linenumber;
					}

					pw.println(out);

				}

			}
			pw.close();
			br.close();
			fr.close();

			System.out.println("OK");

		} catch(FileNotFoundException e) {
			System.err.println("File not found");
		} catch(IOException e) {
			System.err.println("File not found");
		}
	}
}
