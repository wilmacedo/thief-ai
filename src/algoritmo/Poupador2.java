package algoritmo;

import controle.Constantes;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

public class Poupador2 extends ProgramaPoupador {

	class Way {
		Point point;
		Integer det;

		public Way(Point p){
			this.point = p;
			det = 1;
		}

		public Way(Point p, Integer det){
			this.point = p;
			this.det = det;
		}

	}

	private int[] THIEF = { 200, -10};
	private int[] BANK = { 3, 15 };
	private int[] COIN = { 4, 30 };
	private int[] WALL = { 1, -8 };
	private int[] OUTSIDE = { 1, WALL[1] };
	private int[] VOID = { 0, 0 };

	private final int UP = 1;
	private final int DOWN = 2;
	private final int RIGHT = 3;
	private final int LEFT = 4;

	private int[] weightList = null;
	private ArrayList<Way> visitedWays = new ArrayList<>();
	private ArrayList<Way> traveledWays = new ArrayList<>();

	private void generateWeights() {
		weightList = new int[sensor.getVisaoIdentificacao().length];

		ArrayList<int[]> weightTestList = new ArrayList<>(
				Arrays.asList(THIEF, BANK, COIN, WALL, OUTSIDE, VOID)
		);

		for (int i = 0; i < weightList.length; i++) {
			int weight = 0;
			for (int j = 0; j < weightTestList.size(); j++) {
				int vision = sensor.getVisaoIdentificacao()[i];

				if (vision == BANK[0]) {
					if (sensor.getNumeroDeMoedas() <= 0) {
						weight -= 10 * BANK[1];
					} else {
						weight += sensor.getNumeroDeMoedas() * BANK[1]; // Step de medo: Quanto mais moedas maior o peso na determinação de ida ao banco
					}
				}

				if (vision >= THIEF[0] && sensor.getNumeroDeMoedas() > 0) {
					weight += 10 * THIEF[1];
				}

				if (vision == weightTestList.get(j)[0]) {
					weight += weightTestList.get(j)[1];
				}
			}

			weightList[i] = weight;
		}
	}

	public int getTotalWeightBySide(int side) {
		int totalWeight = 0;
		int[] positionList = null;

		switch (side) {
			case DOWN:
				for (int i = 14; i < 24; i++) {
					totalWeight += weightList[i];
				}
				break;
			case LEFT:
				positionList = new int[]{ 0, 5, 10, 14, 19, 1, 6, 11, 15, 20 };
				break;
			case RIGHT:
				positionList = new int[]{ 3, 8, 12, 17, 22, 4, 9, 13, 18, 23 };
				break;
			case UP:
				for (int i = 0; i < 10; i++) {
					totalWeight += weightList[i];
				}
				break;
		}

		if (positionList != null) {
			for (int i = 0; i < positionList.length; i++) {
				totalWeight += weightList[positionList[i]];
			}
		}

		return totalWeight;
	}

	private int getWeightVisited(int x, int y) {
		for(Way way : visitedWays){
			if(way.point.x == x && way.point.y == y){
				return -(way.det*100);
			}
		}
		return 0;
	}

	private int getWeightTraveled(int x, int y) {
		for(Way way : traveledWays){
			if(way.point.x == x && way.point.y == y){
				return -(way.det*100);
			}
		}
		return 0;
	}

	private int decideWay() {
		int way = UP;
		int wX, wY;

		wX = Constantes.posicaoBanco.x - sensor.getPosicao().x;
		wY = Constantes.posicaoBanco.y - sensor.getPosicao().y;

		if (wX > 0) {
			weightList[RIGHT] += sensor.getNumeroDeMoedas() * 100;
		} else if (wX < 0) {
			weightList[LEFT] += sensor.getNumeroDeMoedas()* 100;
		}

		if (wY > 0) {
			weightList[DOWN] += sensor.getNumeroDeMoedas() * 100;
		} else if (wY < 0) {
			weightList[UP] = sensor.getNumeroDeMoedas() * 100;
		}

		int upWeight = getTotalWeightBySide(UP);
		int downWeight = getTotalWeightBySide(DOWN);
		int rightWeight = getTotalWeightBySide(RIGHT);
		int leftWeight = getTotalWeightBySide(LEFT);

		Point actualPos = sensor.getPosicao();
		if (sensor.getVisaoIdentificacao()[UP] != WALL[0] && sensor.getVisaoIdentificacao()[UP] != OUTSIDE[0]) {
			upWeight += getWeightVisited(actualPos.x, actualPos.y - 1);
			upWeight += getWeightTraveled(actualPos.x, actualPos.y - 1);
		}

		if (sensor.getVisaoIdentificacao()[DOWN] != WALL[0] && sensor.getVisaoIdentificacao()[DOWN] != OUTSIDE[0]) {
			downWeight += getWeightVisited(actualPos.x, actualPos.y - 1);
			downWeight += getWeightTraveled(actualPos.x, actualPos.y - 1);
		}

		if (sensor.getVisaoIdentificacao()[RIGHT] != WALL[0] && sensor.getVisaoIdentificacao()[RIGHT] != OUTSIDE[0]) {
			downWeight += getWeightVisited(actualPos.x, actualPos.y - 1);
			downWeight += getWeightTraveled(actualPos.x, actualPos.y - 1);
		}

		if (sensor.getVisaoIdentificacao()[LEFT] != WALL[0] && sensor.getVisaoIdentificacao()[LEFT] != OUTSIDE[0]) {
			downWeight += getWeightVisited(actualPos.x, actualPos.y - 1);
			downWeight += getWeightTraveled(actualPos.x, actualPos.y - 1);
		}

		if (sensor.getVisaoIdentificacao()[UP] == COIN[0]) {
			upWeight += 10;
		}

		if (sensor.getVisaoIdentificacao()[DOWN] == COIN[0]) {
			downWeight += 10;
		}

		if (sensor.getVisaoIdentificacao()[RIGHT] == COIN[0]) {
			rightWeight += 10;
		}

		if (sensor.getVisaoIdentificacao()[LEFT] == COIN[0]) {
			leftWeight += 10;
		}

		if (sensor.getVisaoIdentificacao()[UP] >= THIEF[0] || sensor.getVisaoIdentificacao()[UP] >= 100) {
			downWeight += 30;
		}

		if (sensor.getVisaoIdentificacao()[DOWN] >= THIEF[0] || sensor.getVisaoIdentificacao()[DOWN] >= 100) {
			upWeight += 30;
		}

		if (sensor.getVisaoIdentificacao()[RIGHT] >= THIEF[0] || sensor.getVisaoIdentificacao()[RIGHT] >= 100) {
			leftWeight += 30;
		}

		if (sensor.getVisaoIdentificacao()[LEFT] >= THIEF[0] || sensor.getVisaoIdentificacao()[LEFT] >= 100) {
			rightWeight += 30;
		}

		int[] weightWays = { upWeight, downWeight, rightWeight, leftWeight };
		int higher = -987654321;

		for (int i = 0; i < weightWays.length; i++) {
			if (weightWays[i] > higher) {
				higher = weightWays[i];
				way = (i + 1);
			}
		}

		ArrayList<Integer> equalWeights = new ArrayList<>();
		for (int i = 0; i < weightWays.length; i++) {
			if (weightWays[i] == higher) {
				equalWeights.add(i+1);
			}
		}

		if (equalWeights.size() > 1) {
			Integer i = (int) (Math.random() * (equalWeights.size()));
			way = equalWeights.get(i);
		}

		return way;
	}

	private void updateTraveledWay() {
		Point actualPos = sensor.getPosicao();
		if (traveledWays.size() < 20) {
			traveledWays.add(new Way(actualPos, -60));
		} else {
			traveledWays.remove(0);
			traveledWays.add(new Way(actualPos, -60));
		}
	}

	private void updateVisitedWay() {
		Point actualPos = sensor.getPosicao();
		boolean hasExist = false;
		for (int i = 0; i < visitedWays.size(); i++) {
			if (actualPos.x == visitedWays.get(i).point.x && actualPos.y == visitedWays.get(i).point.y) {
				visitedWays.get(i).det++;
				hasExist = true;
			}
		}

		if (!hasExist) {
			visitedWays.add(new Way(actualPos));
		}
	}

	public int acao() {
		generateWeights();
		int way = decideWay();
		updateVisitedWay();
		updateTraveledWay();

		return way;
	}

}