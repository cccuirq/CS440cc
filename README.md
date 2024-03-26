# CS440 Semester Project

## Battleship Game

Welcome to our implementation of the classic game Battleship. This project is designed to provide a fun and interactive way to play the time-honored game of naval warfare, where the objective is to sink all of your opponent's ships before they sink yours. This README provides all the information you need to get started playing and enjoying Battleship.

### 1) Description

Battleship is a strategy type guessing game for two players. It is played on ruled grids (paper or board) on which each player's fleet of ships (including battleships, submarines, destroyers, and more) are marked. The locations of the fleet are concealed from the other player. Players alternate turns calling "shots" at the other player's ships, and the objective of the game is to destroy the opposing player's fleet.

### 2) How to run

Install Java8

```bash
javac -cp "./lib/*:." @battleship.srcs //for compile
java -cp "./lib/*:." edu.bu.battleship.Main //for running
```
Adding below command to run our smart agent!

```bash
--p1Agent src.pas.battleship.agents.ProbabilisticAgent
```

### 3) More Options

We have three difficulties which corresponds to three different map size and number of ships. You can set difficulty by below code.

```bash
-d EASY
-d MEDIUM
-d HARD
```

To set smarter opponents (Defult Random Agent):

```bash
--p2Agent edu.bu.battleship.agents.EasyAgent
--p2Agent edu.bu.battleship.agents.MediumAgent
--p2Agent edu.bu.battleship.agents.HardAgent
```

To run without rendering the window:

```bash
-s
```

To run with different thingking time (Defult 480000ms):

```bash
-t 10000
```


