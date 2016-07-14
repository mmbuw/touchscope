import pandas as pd
import numpy as np
from scipy import stats as stat
import matplotlib.pyplot as plt
from tabulate import tabulate
import re
import datetime

TIME_COLUMNS = ['time1', 'time2', 'time3']


def to_seconds(t):
    m, s = re.split(':', t)
    seconds = int(datetime.timedelta(minutes=int(m), seconds=int(s)).total_seconds())
    return seconds


def trim_outliers(data):
    # trim outliers that are more than three standard deviations above the mean (Measuring the User Experience p.78)
    for t in TIME_COLUMNS:
        mn = np.mean(data.loc[:, t])
        std = np.std(data.loc[:, t])
        # data.loc[data[t] > (mn + std * 3), t] = mn # fill in with mean
        data.loc[data[t] > (mn + std * 3), t] = np.nan
    return data.dropna(how='any')


def show_distributions(data):
    fig = 1
    for t in TIME_COLUMNS:
        d = sorted(data.loc[:, t])
        mn = np.mean(d)
        std = np.std(d)
        fit = stat.norm.pdf(d, mn, std)

        plt.figure(fig)
        fig += 1
        count, bins, ignored = plt.hist(d, 15, normed=True)
        plt.plot(bins, 1 / (std * np.sqrt(2 * np.pi)) * np.exp(- (bins - mn) ** 2 / (2 * std ** 2)), \
                 linewidth = 2, color = 'r')
        # plt.plot(d, fit, '-o')
        # plt.hist(d, normed=True)
    plt.draw()


def confidence_interval(data):
    confs = []
    for t in TIME_COLUMNS:
        se = stat.sem(data.loc[:, t])
        n = data.loc[:, t].count()
        mn = np.mean(data.loc[:, t])
        conf = stat.t.interval(0.95, n - 1, loc=mn, scale=se)
        confs.append(conf[1] - mn)

    return confs


def basic_stats(data):
    mn = list(np.mean(data.loc[:, TIME_COLUMNS]))
    md = list(data.loc[:, TIME_COLUMNS].apply(np.median))
    small = list(np.min(data.loc[:, TIME_COLUMNS]))
    big = list(np.max(data.loc[:, TIME_COLUMNS]))
    rng = list(data.loc[:, TIME_COLUMNS].apply(lambda x: x.max() - x.min()))
    std = list(np.std(data.loc[:, TIME_COLUMNS]))
    var = list(np.var(data.loc[:, TIME_COLUMNS]))
    conf = confidence_interval(data)

    mn.insert(0, 'mean')
    md.insert(0, 'median')
    small.insert(0, 'min')
    big.insert(0, 'max')
    rng.insert(0, 'range')
    std.insert(0, 'std')
    var.insert(0, 'var')
    conf.insert(0,'95% confidence')

    print(tabulate([small, big, rng, mn, md, var, std, conf], \
                   headers=['time 1', 'time 2', 'time 3'], numalign="right", floatfmt=".2f"))


def main():
    results = pd.read_csv('results.csv')
    for ti in TIME_COLUMNS:
        results[ti] = results[ti].apply(to_seconds)

    results = trim_outliers(results)

    # skew and kurtosis are combined in the normality test
    print(stat.normaltest(results.loc[:, TIME_COLUMNS])[1])
    show_distributions(results)
    basic_stats(results)

    print()

    oscope_results = results[results.device_coded == 0]
    tablet_results = results[results.device_coded == 1]

    print('Oscilloscope ' + str(len(oscope_results)))
    basic_stats(oscope_results)

    print()

    print('Tablet ' + str(len(tablet_results)))
    basic_stats(tablet_results)

    print()

    t, p = list(stat.ttest_ind(oscope_results.loc[:, TIME_COLUMNS], tablet_results.loc[:, TIME_COLUMNS], equal_var=False))
    t = list(t)
    p = list(p)
    t.insert(0, 't')# (' + str(len(results) - 1) + ')')
    p.insert(0, 'p-value')
    print(tabulate([t, p], headers=['time 1', 'time 2', 'time 3'], numalign="right", floatfmt=".3f"))

    plt.show()

main()

