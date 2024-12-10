import pandas as pd
import matplotlib.pyplot as plt

# CSV-Datei einlesen
df = pd.read_csv('log.csv', delimiter=';', decimal=',')

# Graph erstellen
plt.figure(figsize=(20, 10))

# Linien für verschiedene Metriken zeichnen
plt.plot(df['Generation'], df['AverageFitness'], label='Average Fitness')
plt.plot(df['Generation'], df['BestFitness'], label='Best Fitness')
plt.plot(df['Generation'], df['BestOverallFitness'], label='Best Overall Fitness')
plt.plot(df['Generation'], df['MutationRate'], label='Mutation Rate')

# Achsenbeschriftungen und Titel hinzufügen
plt.xlabel('Generation')
plt.ylabel('Values')
plt.title('Genetic Algorithm Performance Metrics')
plt.legend()

# Graph anzeigen
plt.show()