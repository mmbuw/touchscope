import pandas as pd
import numpy as np
from scipy import stats as stat
from tabulate import tabulate
import re
import datetime

TIME_COLUMNS = ['time1', 'time2', 'time3']


def to_seconds(t):
    m,s = re.split(':',t)
    seconds = int(datetime.timedelta(minutes=int(m),seconds=int(s)).total_seconds())
    return seconds


def basic_stats(data):
    for t in TIME_COLUMNS:
        mean = np.mean(data[t])
        median = np.median(data[t])
        min_value = data[t].min()
        max_value = data[t].max()
        rng = max_value - min_value
        var = np.var(data[t])
        std = np.std(data[t])
        print(t)
        print('min = {0}, max = {1}, range = {2}, var = {3:.3f}, std = {4:.3f}'.format(min_value,max_value,rng,var,std))
        print('mean = {0:.3f}, median = {1}'.format(mean, median))


def better(data):
    mn = list(np.mean(data.loc[:, TIME_COLUMNS]))
    mn.insert(0, 'mean')
    md = list(data.loc[:, TIME_COLUMNS].apply(np.median))
    md.insert(0, 'median')
    small = list(np.min(data.loc[:, TIME_COLUMNS]))
    small.insert(0, 'min')
    big = list(np.max(data.loc[:, TIME_COLUMNS]))
    big.insert(0, 'max')
    rng = list(data.loc[:, TIME_COLUMNS].apply(lambda x: x.max() - x.min()))
    rng.insert(0, 'range')
    std = list(np.std(data.loc[:, TIME_COLUMNS]))
    std.insert(0, 'std')
    var = list(np.var(data.loc[:, TIME_COLUMNS]))
    var.insert(0, 'var')
    print(tabulate([small, big, rng, mn, md, std, var], headers=['test 1', 'test 2', 'test 3'], numalign="right", floatfmt=".2f"))



results = pd.read_csv('results.csv')
for t in TIME_COLUMNS:
    results[t] = results[t].apply(to_seconds)

#m = np.array(np.mean(results.loc[:, TIME_COLUMNS]).values)
#n = np.array(results.loc[:, TIME_COLUMNS].apply(np.median).values)
#print(m)
#print(n)

oscope_results = results[results.device_coded == 0]
tablet_results = results[results.device_coded == 1]

print('Oscilloscope')
#basic_stats(oscope_results)
better(oscope_results)

print()

print('Tablet')
#basic_stats(tablet_results)
better(tablet_results)

#mean1 = np.mean(results['task1_coded'])
#mean1 = stat.t.stats(results['task1_coded'].size, moments='m')
#t_result, p_value = stat.ttest_1samp(results['task1_coded'], mean1)


