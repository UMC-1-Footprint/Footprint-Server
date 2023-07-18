package com.umc.footprint.src.walks;

import java.util.ArrayList;
import java.util.List;

import com.umc.footprint.utils.AES128;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CoordinateConvertor {

	public static List<Double> toPoint(String rawPoint) {
		String test = AES128.decrypt(rawPoint);
		if (test.startsWith("POINT")) {
			test = test.substring(5);
		}
		if (test.contains("?")) {
			return new ArrayList<>();
		}
		test = test.substring(1, test.length() - 1);
		String[] sp = test.split(" ");
		List<Double> list = new ArrayList<>();
		list.add(Double.parseDouble(sp[0]));
		list.add(Double.parseDouble(sp[1]));
		return list;
	}

	public static List<ArrayList<Double>> toCoordinates(String rawCoordinates) {

		ArrayList<ArrayList<Double>> resultCoordinates = new ArrayList<>();
		String decryptTest = AES128.decrypt(rawCoordinates);

		if (decryptTest.contains("MULTILINESTRING")) {
			decryptTest = decryptTest.substring(17, decryptTest.length() - 2); //MULTISTRING((, ) split
		}

		decryptTest = decryptTest.substring(1, decryptTest.length() - 1); // 앞 뒤 괄호 제거
		String[] strArr = decryptTest.split("\\),");

		for (String coor : strArr) {
			log.info("hello loop");
			coor = coor.replace("(", "");
			coor = coor.replace(")", "");

			String[] comma = coor.split(",");
			ArrayList<Double> temp = new ArrayList<>();
			for (String com : comma) {
				String[] space = com.split(" ");
				double x = Double.parseDouble(space[0]);
				if (x <= 10) {
					x += 30;
				}
				temp.add(x);
				temp.add(Double.parseDouble(space[1]));
			}
			resultCoordinates.add(temp);
		}
		return resultCoordinates;
	}

	public static String fromCoordinates(List<List<Double>> coordinates) {

		log.debug("String으로 변환할 리스트: {}", coordinates);
		ArrayList<List<Double>> safeCoordinates = toSafeCoordinates(coordinates);

		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("(");
		int count = 0;  // 1차원 범위의 List 경과 count (마지막 "," 빼기 위해)
		for (List<Double> list : safeCoordinates) {
			stringBuilder.append("(");
			for (int i = 0; i < list.size(); i++) {
				stringBuilder.append(list.get(i));

				if (i == list.size() - 1) {    // 마지막은 " " , "," 추가하지 않고 ")"
					stringBuilder.append(")");
					break;
				}

				if (i % 2 == 0)   // 짝수 번째 인덱스는 " " 추가
					stringBuilder.append(" ");
				else            // 홀수 번째 인덱스는 "," 추가
					stringBuilder.append(",");
			}
			count++;
			if (count != safeCoordinates.size())    // 1차원 범위의 List에서 마지막을 제외하고 "," 추가
				stringBuilder.append(",");
		}
		stringBuilder.append(")");

		return stringBuilder.toString();
	}

	public static String fromPoint(List<Double> inputList) {
		log.debug("string 형으로 바꿀 list: {} ", inputList);

		if (inputList.isEmpty()) {
			return "(?  ?)";
		}

		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("(");
		stringBuilder.append(inputList.get(0));
		stringBuilder.append(" ");
		stringBuilder.append(inputList.get(1));
		stringBuilder.append(")");
		String result = stringBuilder.toString();

		return result;
	}

	private static ArrayList<List<Double>> toSafeCoordinates(List<List<Double>> coordinates) {
		ArrayList<List<Double>> safeCoordinate = new ArrayList<>();
		for (List<Double> line : coordinates) {
			// 좌표가 하나만 있는 라인이 있을 때
			log.debug("line: {}", line);
			if (line.size() == 2) {
				line.add(line.get(0));
				line.add(line.get(1));
			}
			safeCoordinate.add(line);
		}
		return safeCoordinate;
	}
}
