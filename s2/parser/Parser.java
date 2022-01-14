package enshud.s2.parser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class Parser {
	public LinkedList<String> key = new LinkedList<String>();
	public LinkedList<Integer> linenumber = new LinkedList<Integer>();
	boolean err = false;
	int errlinenumber;
	int firsterrlinenumber;

	/**
	 * サンプルmainメソッド．
	 * 単体テストの対象ではないので自由に改変しても良い．
	 */
	public static void main(final String[] args) {
		// normalの確認
		new Parser().run("data/ts/normal01.ts");
		new Parser().run("data/ts/normal02.ts");

		// synerrの確認
		new Parser().run("data/ts/synerr01.ts");
		new Parser().run("data/ts/synerr02.ts");
	}

	/**
	 * TODO
	 * 
	 * 開発対象となるParser実行メソッド．
	 * 以下の仕様を満たすこと．
	 * 
	 * 仕様:
	 * 第一引数で指定されたtsファイルを読み込み，構文解析を行う．
	 * 構文が正しい場合は標準出力に"OK"を，正しくない場合は"Syntax error: line"という文字列とともに，
	 * 最初のエラーを見つけた行の番号を標準エラーに出力すること （例: "Syntax error: line 1"）．
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
				key.add(linesplit[1]);
				linenumber.add(Integer.parseInt(linesplit[3]));
				line = br.readLine();
			} while (line != null);

			program();

			if (err) {
				System.err.print("Syntax error: line ");
				System.err.println(firsterrlinenumber);
			} else {
				System.out.println("OK");
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
		return key.pop();
	}

	public void error() {
		if (err == false) {
			firsterrlinenumber = errlinenumber;
		}
		err = true;
	}

	public void program() {
		if (! next().equals("SPROGRAM")) {
			error(); // まず "program" という文字が出てこなければ構文エラー
		}

		programName(); // 別のEBNFメソッドを呼び出して解析を続ける

		if (! next().equals("SSEMICOLON")) {
			error(); // プログラム名の次に ";" が出てこなければ構文エラー
		}

		block();
		complexStatement();

		if (! next().equals("SDOT")) {
			error();
		}
	}

	public void programName() {
		if (! next().equals("SIDENTIFIER")) {
			error();
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
		} else {
			variableDeclarations();
		}
	}

	public void variableDeclarations() {
		variableNames();

		if (! next().equals("SCOLON")) {
			error();
		}

		type();

		if (! next().equals("SSEMICOLON")) {
			error();
		}

		while (true) {
			String tmp = next();
			if (! tmp.equals("SIDENTIFIER")) {
				key.push(tmp);
				linenumber.push(errlinenumber);
				break;
			} else {
				key.push(tmp);
				linenumber.push(errlinenumber);

				variableNames();

				if (! next().equals("SCOLON")) {
					error();
				}

				type();

				if (! next().equals("SSEMICOLON")) {
					error();
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
				break;
			} else {
				variableName();
			}
		}
	}

	public void variableName() {
		if (! next().equals("SIDENTIFIER")) {
			error();
		}
	}

	public void type() {
		String tmp = next();
		if (! tmp.equals("SARRAY")) {
			key.push(tmp);
			linenumber.push(errlinenumber);
			standardType();
		} else {
			key.push(tmp);
			linenumber.push(errlinenumber);
			arrayType();
		}
	}

	public void standardType() {
		if (! next().matches("SINTEGER|SCHAR|SBOOLEAN")) {
			error();
		}
	}

	public void arrayType() {
		if (! next().equals("SARRAY")) {
			error();
		}
		if (! next().equals("SLBRACKET")) {
			error();
		}
		minimumSubscript();
		if (! next().equals("SRANGE")) {
			error();
		}
		maximumSubscript();
		if (! next().equals("SRBRACKET")) {
			error();
		}
		if (! next().equals("SOF")) {
			error();
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
				error();
			}
		} else {
			if (! next().equals("SCONSTANT")) {
				error();
			}
		}
	}

	public void sign() {
		if (! next().matches("SPLUS|SMINUS")) {
			error();
		}
	}

	public void subProgramDeclarationGroup() {
		while (true) {
			String tmp = next();
			if (! tmp.equals("SPROCEDURE")) {
				key.push(tmp);
				linenumber.push(errlinenumber);
				break;
			} else {
				key.push(tmp);
				linenumber.push(errlinenumber);
				subProgramDeclaration();
				if (! next().equals("SSEMICOLON")) {
					error();
				}
			}
		}
	}

	public void subProgramDeclaration() {
		subProgramHead();
		variableDeclaration();
		complexStatement();
	}

	public void subProgramHead() {
		if (! next().equals("SPROCEDURE")) {
			error();
		}

		procedureName();

		temporaryParameter();

		if (! next().equals("SSEMICOLON")) {
			error();
		}
	}

	public void procedureName() {
		if (! next().equals("SIDENTIFIER")) {
			error();
		}
	}

	public void temporaryParameter() {
		String tmp = next();
		if (! tmp.equals("SLPAREN")) {
			key.push(tmp);
			linenumber.push(errlinenumber);
		} else {
			temporaryParameters();
			if (! next().equals("SRPAREN")) {
				error();
			}
		}
	}

	public void temporaryParameters() {
		temporaryParameterNames();

		if (! next().equals("SCOLON")) {
			error();
		}

		standardType();

		while (true) {
			String tmp = next();
			if (! tmp.equals("SSEMICOLON")) {
				key.push(tmp);
				linenumber.push(errlinenumber);
				break;
			} else {
				temporaryParameterNames();

				if (! next().equals("SCOLON")) {
					error();
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
				break;
			} else {
				temporaryParameterName();
			}
		}
	}

	public void temporaryParameterName() {
		if (! next().equals("SIDENTIFIER")) {
			error();
		}
	}

	public void complexStatement() {
		if (! next().equals("SBEGIN")) {
			error();
		}

		statements();

		if (! next().equals("SEND")) {
			//System.out.println("ここかな");
			error();
		}
	}

	public void statements() {
		statement();

		while (true) {
			String tmp = next();
			if (! tmp.equals("SSEMICOLON")) {
				key.push(tmp);
				linenumber.push(errlinenumber);
				break;
			} else {
				statement();
			}
		}
	}

	public void statement() {
		String tmp = next();
		if (tmp.equals("SIF")) {
			formula();
			if (! next().equals("STHEN")) {
				error();
			}
			complexStatement();
			tmp = next();
			if (! tmp.equals("SELSE")) {
				key.push(tmp);
				linenumber.push(errlinenumber);
			} else {
				complexStatement();
			}
		} else if (tmp.equals("SWHILE")) {
			formula();
			if (! next().equals("SDO")) {
				error();
			}
			statement();
		} else {
			key.push(tmp);
			linenumber.push(errlinenumber);
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
				key.push(tmp1);
				linenumber.push(tmp2);
				assignStatement();
			} else if(tmp3.matches("SLPAREN|SSEMICOLON|SEND")) {
				key.push(tmp3);
				linenumber.push(errlinenumber);
				key.push(tmp1);
				linenumber.push(tmp2);
				procedureCallingStatement();
			} else {
				error();
			}
		} else if (tmp1.matches("SREADLN|SWRITELN")) {
			key.push(tmp1);
			linenumber.push(tmp2);
			ioStatement();
		} else if (tmp1.equals("SBEGIN")) {
			key.push(tmp1);
			linenumber.push(tmp2);
			complexStatement();
		} else if (tmp1.equals("SEND")) {
			key.push(tmp1);
			linenumber.push(tmp2);
		} else {
			error();
		}
	}

	public void assignStatement() {
		leftSide();

		if (! next().equals("SASSIGN")) {
			error();
		}

		formula();
	}

	public void leftSide() {
		variable();
	}

	public void variable() {
		if (! next().equals("SIDENTIFIER")) {
			error();
		}
		String tmp = next();
		if (! tmp.equals("SLBRACKET")) {
			key.push(tmp);
			linenumber.push(errlinenumber);
		} else {
			subscript();
			if (! next().equals("SRBRACKET")) {
				error();
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
		} else {
			formulas();
			if (! next().equals("SRPAREN")) {
				error();
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
		}
	}

	public void simpleFormula() {
		String tmp = next();
		if (! tmp.matches("SPLUS|SMINUS")) {
			key.push(tmp);
			linenumber.push(errlinenumber);
		}
		term();
		while (true) {
			tmp = next();
			if (! tmp.matches("SPLUS|SMINUS|SOR")) {
				key.push(tmp);
				linenumber.push(errlinenumber);
				break;
			} else {
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
				break;
			} else {
				factor();
			}
		}
	}

	public void factor() {
		String tmp = next();
		if (tmp.equals("SIDENTIFIER")) {
			key.push(tmp);
			linenumber.push(errlinenumber);
			variable();
		} else if (tmp.equals("SLPAREN")) {
			formula();
			if (! next().equals("SRPAREN")) {
				error();
			}
		} else if (tmp.equals("SNOT")) {
			factor();
		} else {
			key.push(tmp);
			linenumber.push(errlinenumber);
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
			} else {
				variables();
				tmp = next();
				if (! tmp.equals("SRPAREN")) {
					error();
				}
			}
		} else if (tmp.equals("SWRITELN")) {
			tmp = next();
			if (! tmp.equals("SLPAREN")) {
				key.push(tmp);
				linenumber.push(errlinenumber);
			} else {
				formulas();
				tmp = next();
				if (! tmp.equals("SRPAREN")) {
					error();
				}
			}
		} else {
			error();
		}
	}

	public void variables() {
		variable();
		while (true) {
			String tmp = next();
			if (! tmp.equals("SCOMMA")) {
				key.push(tmp);
				linenumber.push(errlinenumber);
				break;
			} else {
				variable();
			}
		}
	}

	public void constant() {
		if (! next().matches("SCONSTANT|SSTRING|SFALSE|STRUE")) {
			error();
		}
	}
}