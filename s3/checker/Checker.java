package enshud.s3.checker;

import java.util.Arrays;
import java.util.LinkedList;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Checker {
	public LinkedList<String> key = new LinkedList<String>();
	public LinkedList<Integer> linenumber = new LinkedList<Integer>();
	public LinkedList<String> raw = new LinkedList<String>();
	public LinkedList<String> names = new LinkedList<String>();
	public LinkedList<Integer> attribute = new LinkedList<Integer>();
	public LinkedList<String> subprogramnames = new LinkedList<String>();

	boolean synerr = false;
	boolean semerr = false;
	int errlinenumber;
	int firstsynerrlinenumber;
	int firstsemerrlinenumber;
	int current = -1;
	int state = 0;



	/**
	 * サンプルmainメソッド．
	 * 単体テストの対象ではないので自由に改変しても良い．
	 */
	public static void main(final String[] args) {
		// normalの確認
		new Checker().run("data/ts/normal01.ts");
		new Checker().run("data/ts/normal02.ts");

		// synerrの確認
		new Checker().run("data/ts/synerr01.ts");
		new Checker().run("data/ts/synerr02.ts");

		// semerrの確認
		new Checker().run("data/ts/semerr01.ts");
		new Checker().run("data/ts/semerr02.ts");
	}

	/**
	 * TODO
	 * 
	 * 開発対象となるChecker実行メソッド．
	 * 以下の仕様を満たすこと．
	 * 
	 * 仕様:
	 * 第一引数で指定されたtsファイルを読み込み，意味解析を行う．
	 * 意味的に正しい場合は標準出力に"OK"を，正しくない場合は"Semantic error: line"という文字列とともに，
	 * 最初のエラーを見つけた行の番号を標準エラーに出力すること （例: "Semantic error: line 6"）．
	 * また，構文的なエラーが含まれる場合もエラーメッセージを表示すること（例： "Syntax error: line 1"）．
	 * 入力ファイル内に複数のエラーが含まれる場合は，最初に見つけたエラーのみを出力すること．
	 * 入力ファイルが見つからない場合は標準エラーに"File not found"と出力して終了すること．
	 * 
	 * @param inputFileName 入力tsファイル名
	 */
	public void run(final String inputFileName) {
		FileReader fr;
		BufferedReader br;
		try {
			fr = new FileReader(inputFileName);
			br = new BufferedReader(fr);

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
				System.out.println(attribute);
				System.out.println(names);
			} else {
				System.out.println("OK");
//				System.out.println(attribute);
//				System.out.println(names);
			}

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
//		System.out.println(raw.get(current));
//		String tmp = key.pop();
//		System.out.println(tmp);
//		return tmp;

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
	}

	public void programName() {
		if (! next().equals("SIDENTIFIER")) {
			synerror();
		}
	}

	public void block() {
		variableDeclaration();
		subProgramDeclarationGroup();
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

		//names.clear();
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
		String tmp = raw.get(current+1);
		if (! next().equals("SIDENTIFIER")) {
			synerror();
		} else if (state == 0) {
			for (String name : names) {
				if (name.equals(tmp)) {
					System.out.println("名前の重複エラー");
					semerror();
				}
			}
			names.add(tmp);

			String tmp2 = key.pop();
			String tmp3 = key.pop();

			if (tmp3.equals("SBOOLEAN")) {
				attribute.add(1);
			} else if (tmp3.equals("SARRAY")) {
				attribute.add(2);
			} else if (tmp3.equals("SCHAR")) {
				attribute.add(3);
			} else {
				attribute.add(0);
			}

			key.push(tmp3);
			key.push(tmp2);
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
		if (! next().matches("SINTEGER|SCHAR|SBOOLEAN")) {
			synerror();
		}
	}

	public void arrayType() {
		if (! next().equals("SARRAY")) {
			synerror();
		}
		if (! next().equals("SLBRACKET")) {
			synerror();
		}
		minimumSubscript();
		if (! next().equals("SRANGE")) {
			synerror();
		}
		maximumSubscript();
		if (! next().equals("SRBRACKET")) {
			synerror();
		}
		if (! next().equals("SOF")) {
			synerror();
		}
		standardType();
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
		state = 1;
		subProgramHead();
		variableDeclaration();
		complexStatement();
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

		standardType();

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

				standardType();
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
		String tmp = raw.get(current+1);
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

			int i = 0;
			for (String name : names) {
				if (name.equals(raw.get(current+1))) {
					if (attribute.get(i) != 1 && !raw.get(current+2).matches("=|<>|<|<=|>|>=|\\+|-|or|\\*|/|div|mod|and|\\[")) {
						System.out.println(raw.get(current+1));
						semerror();
					}
				}
				i++;
			}

			formula();
			if (! next().equals("STHEN")) {
				synerror();
			}
			complexStatement();
			tmp = next();
			if (! tmp.equals("SELSE")) {
				key.push(tmp);
				linenumber.push(errlinenumber);
				current--;
			} else {
				complexStatement();
			}
		} else if (tmp.equals("SWHILE")) {
			formula();
			if (! next().equals("SDO")) {
				synerror();
			}
			statement();
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
//			System.out.println(errlinenumber);
//			System.out.println(tmp2);
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
		leftSide();

		if (! next().equals("SASSIGN")) {
			synerror();
		}

		formula();
	}

	public void leftSide() {
		boolean ok = false;
		int i = 0;
		for (String name: names) {
			if (raw.get(current+1).equals(name)) {
				if (attribute.get(i) != 2) {
					ok = true;
				} else if (raw.get(current+2).equals("[")) {
					ok = true;
				}
			}
			i++;
		}

		if ((ok == false) && (state == 0)) {
			//System.out.println("宣言されていない変数");
			semerror();
		}

		i = 0;

		for (String name: names) {
			if (raw.get(current+1).equals(name)) {
				int j = 0;
				for (String name2: names) {
					if (raw.get(current+3).equals(name2)) {
						if ((attribute.get(i) != attribute.get(j)) && (attribute.get(i) != 2) && (attribute.get(j) != 2)) {
							semerror();
						}
					}
					j++;
				}
			}
			i++;
		}


		variable();
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
		} else {
			state = 2;
			subscript();
			if (! next().equals("SRBRACKET")) {
				synerror();
			}
		}
	}

	public void subscript() {
		formula();
	}

	public void procedureCallingStatement() {
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
	}

	public void formulas() {
		formula();

		while (true) {
			String tmp = next();
			if (! tmp.equals("SCOMMA")) {
				key.push(tmp);
				linenumber.push(errlinenumber);
				current--;
				break;
			} else {
				formula();
			}
		}
	}

	public void formula() {
		simpleFormula();

		String tmp = next();
		if (tmp.matches("SEQUAL|SNOTEQUAL|SLESS|SLESSEQUAL|SGREAT|SGREATEQUAL")) {
			simpleFormula();
		} else {
			key.push(tmp);
			linenumber.push(errlinenumber);
			current--;
		}
	}

	public void simpleFormula() {
		String tmp = next();
		if (! tmp.matches("SPLUS|SMINUS")) {
			key.push(tmp);
			linenumber.push(errlinenumber);
			current--;
		}
		term();
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
				term();
			}
		}
	}

	public void term() {
		factor();
		while (true) {
			String tmp = next();
			if (! tmp.matches("SSTAR|SDIVD|SMOD|SAND")) {
				key.push(tmp);
				linenumber.push(errlinenumber);
				current--;
				break;
			} else {
				factor();
			}
		}
	}

	public void factor() {
		String tmp = next();
		if (tmp.equals("SIDENTIFIER")) {
			if (state == 2) {
				int i = 0;
				for (String name : names) {
					if (name.equals(raw.get(current))) {
						if (attribute.get(i) == 1 && attribute.get(i-2) == 2) {
							semerror();
						}
					}
					i++;
				}
				state = 1;
			}

			key.push(tmp);
			linenumber.push(errlinenumber);
			current--;
			variable();
		} else if (tmp.equals("SLPAREN")) {
			formula();
			if (! next().equals("SRPAREN")) {
				synerror();
			}
		} else if (tmp.equals("SNOT")) {
			factor();
		} else {
			key.push(tmp);
			linenumber.push(errlinenumber);
			current--;
			constant();
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
				formulas();
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

	public void constant() {
		String tmp = next();
		if (!tmp.matches("SCONSTANT|SSTRING|SFALSE|STRUE")) {
			synerror();
		}
	}

}
