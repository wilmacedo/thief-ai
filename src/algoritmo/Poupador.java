package algoritmo;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import controle.Constantes;

public class Poupador extends ProgramaPoupador {

    class Way {
        Point point;
        int det;

        public Way(Point p){
            this.point = p;
            det = 1;
        }

        public Way(Point p, int det){
            this.point = p;
            this.det = det;
        }
    }

    final int WITHOUT_VISION = -2;
    final int WITHOUT_VISION_WEIGHT = -100;

    final int OUTSIDE = -1;
    final int OUTSIDE_WEIGHT = -800;

    final int VOID = 0;
    final int VOID_WEIGHT = 0;

    final int WALL = 1;
    final int WALL_WEIGHT = -800;

    final int BANK = 3;
    final int BANK_WEIGHT = 1500;

    final int COIN = 4;
    final int COIN_WEIGHT = 3000;

    final int POWER_CELL = 5;
    final int POWER_CELL_WEIGHT = -300;

    final int POUPADOR = 100;

    final int THIEF = 200;
    final int THIEF_WEIGHT = -1000;

    final int UP = 7;
    final int LEFT = 11;
    final int RIGHT = 12;
    final int DOWN = 16;

    private ArrayList<Way> visitedWays = new ArrayList<>();
    private ArrayList<Way> traveledWays = new ArrayList<>();
    private int[] weightList = null;

    public void defineVisionWeight(){
        weightList = new int[sensor.getVisaoIdentificacao().length];
        for (int i = 0; i < sensor.getVisaoIdentificacao().length; i++) {
            int weight = 0;
            ArrayList<int[]> weightTestList = new ArrayList<>(
                    Arrays.asList(new int[]{ COIN, COIN_WEIGHT }, new int[] { POWER_CELL, POWER_CELL_WEIGHT }, new int[] { OUTSIDE, OUTSIDE_WEIGHT }, new int[] { WITHOUT_VISION, WITHOUT_VISION_WEIGHT })
            );

            for (int j = 0; j < weightTestList.size(); j++) {
                if (sensor.getVisaoIdentificacao()[i] == weightTestList.get(j)[0]) {
                    weight += weightTestList.get(j)[1];
                }

                if (sensor.getVisaoIdentificacao()[i] == BANK) {
                    if (sensor.getNumeroDeMoedas() > 0) {
                        weight += sensor.getNumeroDeMoedas() * BANK_WEIGHT;
                    } else {
                        weight += 10 * BANK_WEIGHT;
                    }
                }

                if (sensor.getVisaoIdentificacao()[i] >= THIEF) {
                    if (sensor.getNumeroDeMoedas() > 0) {
                        weight += 10 * THIEF_WEIGHT;
                    } else {
                        weight += THIEF_WEIGHT;
                    }
                }
            }
            weightList[i] = weight;
        }
    }

    public int getTotalWeightSide(int side) {
        int totalWeight = 0;
        int[] positionList = null;

        switch (side) {
            case UP:
                for (int i = 0; i < 10; i++) {
                    totalWeight += weightList[i];
                }
                break;
            case DOWN:
                for (int i = 14; i < 24; i++) {
                    totalWeight += weightList[i];
                }
                break;
            case RIGHT:
                positionList = new int[] { 3, 8, 12, 17, 22, 4, 9, 13, 18, 23 };
                break;
            case LEFT:
                positionList = new int[] { 0, 5, 10, 14, 19, 1, 6, 11, 15, 20 };
                break;
        }

        if (positionList != null) {
            for (int i = 0; i < positionList.length; i++) {
                totalWeight += weightList[positionList[i]];
            }
        }

        return totalWeight;
    }

    public ArrayList<int[]> getWeightByPosition() {
        Point actualPosition = sensor.getPosicao();
        ArrayList<int[]> calculatedPositions = new ArrayList<>(
                Arrays.asList(
                        new int[] { DOWN, getTotalWeightSide(DOWN) },
                        new int[] { LEFT, getTotalWeightSide(LEFT) },
                        new int[] { RIGHT, getTotalWeightSide(RIGHT) },
                        new int[] { UP, getTotalWeightSide(UP) })
        );

        for (int i = 0; i < calculatedPositions.size(); i++) {
            int[] pos = calculatedPositions.get(i);

            if (sensor.getVisaoIdentificacao()[pos[0]] != WALL && sensor.getVisaoIdentificacao()[pos[0]] != OUTSIDE) {
                pos[1] += trazerPesoPosicao(actualPosition.x, actualPosition.y);
                pos[1] += trazerPesoCaminho(actualPosition.x, actualPosition.y);
            }
        }

        return calculatedPositions;
    }

    public ArrayList<int[]> verifyPowerCellWeight(ArrayList<int[]> calculatedPositions) {
        int[] directions = new int[] { DOWN, LEFT, RIGHT, UP };
        for (int i = 0; i < directions.length; i++) {
            if (sensor.getVisaoIdentificacao()[directions[i]] == POWER_CELL && (sensor.getNumeroDeMoedas() < 10 || sensor.getNumeroJogadasImunes() > 0)) {
                calculatedPositions.get(i)[1] -= 10000;
            }
        }

        return calculatedPositions;
    }

    public ArrayList<int[]> verifyBlockWayWeight(ArrayList<int[]> calculatedPositions) {
        int[] directions = new int[] { DOWN, LEFT, RIGHT, UP };
        for (int i = 0; i < directions.length; i++) {
            if (sensor.getVisaoIdentificacao()[directions[i]] == WALL || sensor.getVisaoIdentificacao()[directions[i]] == OUTSIDE) {
                calculatedPositions.get(i)[1] -= 15000;
            }
        }

        return calculatedPositions;
    }

    public int decideWay(){
        int way = 0;
        int wayX, wayY;

        wayX = Constantes.posicaoBanco.x - sensor.getPosicao().x;
        wayY = Constantes.posicaoBanco.y - sensor.getPosicao().y;
        int wayPos = sensor.getNumeroDeMoedas() * 100;

        if (wayX > 0) {
            weightList[RIGHT] += wayPos;
        } else if (wayX < 0) {
            weightList[LEFT] += wayPos;
        }

        if (wayY > 0) {
            weightList[DOWN] += wayPos;
        } else if (wayY < 0) {
            weightList[UP] += wayPos;
        }

        ArrayList<int[]> calculatedPositions = getWeightByPosition();
        calculatedPositions = verifyPowerCellWeight(calculatedPositions);
        calculatedPositions = verifyBlockWayWeight(calculatedPositions);

        int[] directions = new int[] { DOWN, LEFT, RIGHT, UP };
        for (int i = 0; i < directions.length; i++) {
            if (sensor.getVisaoIdentificacao()[directions[i]] == COIN) {
                calculatedPositions.get(i)[1] += 1000;
            }
        }

        int pesoTotalBaixo    = calculatedPositions.get(0)[1];
        int pesoTotalEsquerda = calculatedPositions.get(1)[1];
        int pesoTotalDireita  = calculatedPositions.get(2)[1];
        int pesoTotalCima     = calculatedPositions.get(3)[1];

        if(sensor.getVisaoIdentificacao()[UP]>=THIEF || sensor.getVisaoIdentificacao()[UP]>=POUPADOR){
            pesoTotalBaixo += 3000;
        }
        if(sensor.getVisaoIdentificacao()[DOWN]>=THIEF || sensor.getVisaoIdentificacao()[DOWN]>=POUPADOR){
            pesoTotalCima += 3000;
        }
        if(sensor.getVisaoIdentificacao()[RIGHT]>=THIEF || sensor.getVisaoIdentificacao()[RIGHT]>=POUPADOR){
            pesoTotalEsquerda += 3000;
        }
        if(sensor.getVisaoIdentificacao()[LEFT]>=THIEF || sensor.getVisaoIdentificacao()[LEFT]>=POUPADOR){
            pesoTotalDireita += 3000;
        }

        //1 2 3 4
        int[] pesosDirecao = { pesoTotalCima, pesoTotalBaixo, pesoTotalDireita, pesoTotalEsquerda };
        Integer maior = -987654321;

        for (int i = 0; i < pesosDirecao.length; i++) {
            if (pesosDirecao[i] > maior) {
                maior = pesosDirecao[i];
                way = (i+1);
            }
        }
        //Verifica se existe mais de um peso com o mesmo valor
        ArrayList<Integer> pesosIguais = new ArrayList<Integer>();
        for (int i = 0; i < pesosDirecao.length; i++) {
            if (pesosDirecao[i] == maior) {
                pesosIguais.add(i+1);
            }
        }

        //Verifica se Existe mais de um peso na lista, se hover faz um randon
        if (pesosIguais.size() > 1) {
            Integer i = (int) (Math.random() * (pesosIguais.size()));
            way = pesosIguais.get(i);
        }

        return way;
    }

    public void atualizarPontosVisitados(){
        Point posicaoAtual = sensor.getPosicao();
        boolean existe = false;
        for (int i = 0; i < traveledWays.size(); i++) {
            if(posicaoAtual.x == traveledWays.get(i).point.x && posicaoAtual.y == traveledWays.get(i).point.y){
                traveledWays.get(i).det++;
                existe = true;
            }
        }
        if(!existe){
            traveledWays.add(new Way(posicaoAtual));
        }
    }

    public Integer trazerPesoPosicao(Integer x, Integer y){
        for(Way way : visitedWays){
            if(way.point.x == x && way.point.y == y){
                return -(way.det*100);
            }
        }
        return 0;
    }

    public void updateTraveledWays(){
        Point posicaoAtual = sensor.getPosicao();
        if(traveledWays.size() < 40){
            traveledWays.add(new Way(posicaoAtual,-6000));
        }else{
            traveledWays.remove((0));
            traveledWays.add(new Way(posicaoAtual,-6000));
        }
    }

    public Integer trazerPesoCaminho(Integer x, Integer y){
        for(Way way : traveledWays){
            if(way.point.x == x && way.point.y == y){
                return way.det;
            }
        }
        return 0;
    }

    public void definirCheiroTHIEF(){
        int[] olfato = sensor.getAmbienteOlfatoLadrao();
        for(int i=0; i<olfato.length;i++){
            if(i == 0 ){
                weightList[6] += (olfato[i]/-1000);
            }
            if(i == 1 ){
                weightList[7] += (olfato[i]/-1000);
            }
            if(i == 2 ){
                weightList[8] += (olfato[i]/-1000);
            }
            if(i == 3 ){
                weightList[11] += (olfato[i]/-1000);
            }
            if(i == 4 ){
                weightList[12] += (olfato[i]/-1000);
            }
            if(i == 5 ){
                weightList[15] += (olfato[i]/-1000);
            }
            if(i == 6 ){
                weightList[16] += (olfato[i]/-1000);
            }
            if(i == 7 ){
                weightList[17] += (olfato[i]/-1000);
            }
        }
    }

    public void definirCheiroPoupador(){
        int[] olfato = sensor.getAmbienteOlfatoLadrao();
        for(int i=0; i<olfato.length;i++){
            if(i == 0 ){
                weightList[6] += (olfato[i]/-500);
            }
            if(i == 1 ){
                weightList[7] += (olfato[i]/-500);
            }
            if(i == 2 ){
                weightList[8] += (olfato[i]/-500);
            }
            if(i == 3 ){
                weightList[11] += (olfato[i]/-500);
            }
            if(i == 4 ){
                weightList[12] += (olfato[i]/-500);
            }
            if(i == 5 ){
                weightList[15] += (olfato[i]/-500);
            }
            if(i == 6 ){
                weightList[16] += (olfato[i]/-500);
            }
            if(i == 7 ){
                weightList[17] += (olfato[i]/-500);
            }
        }
    }

    public int acao() {
        int acaoRetorno = 0;

        defineVisionWeight();
        definirCheiroTHIEF();
        definirCheiroPoupador();
        acaoRetorno = decideWay();
        atualizarPontosVisitados();
        updateTraveledWays();

        return acaoRetorno;
    }
}