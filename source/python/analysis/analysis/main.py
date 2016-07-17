import pandas as pd
import numpy as np
from scipy import stats as stat
import matplotlib.pyplot as plt
from tabulate import tabulate
import re
import datetime
from matplotlib import colors

TIME_COLUMNS = ['time1', 'time2', 'time3']
RESULTS_COLUMNS = ['task1_coded', 'task2_coded', 'task3_coded']


def to_seconds(t):
    m, s = re.split(':', t)
    seconds = int(datetime.timedelta(minutes=int(m), seconds=int(s)).total_seconds())
    return seconds


def trim_outliers(data):
    ''' trim outliers that are more than three standard deviations above the mean 
     (Measuring the User Experience p.78)
     '''
    
    for t in TIME_COLUMNS:
        mn = np.mean(data.loc[:, t])
        std = np.std(data.loc[:, t])
        # data.loc[data[t] > (mn + std * 3), t] = mn # fill in with mean
        data.loc[data[t] > (mn + std * 3), t] = np.nan
    before = len(data)
    data = data.dropna(how='any')
    after = len(data)
    print(str(before - after) + ' outliers removed\n')
    return data


def show_distributions(data):
    fig = 1
    for col in data:
        d = sorted(data.loc[:, col])
        mn = np.mean(d)
        std = np.std(d)
        fit = stat.norm.pdf(d, mn, std)

        plt.figure(fig)
        fig += 1
        count, bins, ignored = plt.hist(d, 15, normed=True)
        plt.plot(bins, 1 / (std * np.sqrt(2 * np.pi)) * np.exp(- (bins - mn) ** 2 / (2 * std ** 2)),
                 linewidth=2, color='r')
        # plt.plot(d, fit, '-o')
        # plt.hist(d, normed=True)
    plt.draw()


def confidence_interval(data):
    confs = []
    for col in data:
        se = stat.sem(data.loc[:, col])
        n = data.loc[:, col].count()
        mn = np.mean(data.loc[:, col])
        conf = stat.t.interval(0.95, n - 1, loc=mn, scale=se)
        confs.append(conf[1] - mn)

    return confs


def basic_stats(data, headers):
    mn = list(np.mean(data))
    md = list(data.apply(np.median))
    mo = stat.mode(data)[0].tolist()[0]
    small = list(np.min(data))
    big = list(np.max(data))
    rng = list(data.apply(lambda x: x.max() - x.min()))
    std = list(np.std(data))
    var = list(np.var(data))
    conf = confidence_interval(data)

    mn.insert(0, 'mean')
    md.insert(0, 'median')
    mo.insert(0, 'mode')
    small.insert(0, 'min')
    big.insert(0, 'max')
    rng.insert(0, 'range')
    std.insert(0, 'std')
    var.insert(0, 'var')
    conf.insert(0, '95% confidence')

    print(tabulate([small, big, rng, mn, md, mo, var, std, conf],
                   headers=[h for h in headers], numalign="right", floatfmt=".2f"))
    print()


def t_test_ind(data1, data2, headers):
    t, p = list(stat.ttest_ind(data1, data2, equal_var=False))
    t = list(t)
    p = list(p)
    t.insert(0, 't')  # (' + str(len(results) - 1) + ')')
    p.insert(0, 'p-value')
    print(tabulate([t, p], headers=[h for h in headers], numalign="right", floatfmt=".3f"))
    print()


def show_bar_graph(o_data, t_data, ylabel, xlabel, title, ticks):
    mn1 = list(np.mean(o_data))
    conf1 = confidence_interval(o_data)

    mn2 = list(np.mean(t_data))
    conf2 = confidence_interval(t_data)

    index = np.arange(3)
    bar_width = 0.20
    error_config = {'ecolor': '0'}

    fig, ax = plt.subplots()

    plt.bar(index + bar_width, mn1, bar_width,
            color='b', yerr=conf1, error_kw=error_config, label='Oscilloscope')
    plt.bar(index + bar_width*2, mn2, bar_width,
            color='r', yerr=conf2, error_kw=error_config, label='Tablet')

    plt.xlabel(xlabel)
    plt.ylabel(ylabel)
    plt.title(title)
    plt.xticks(index + bar_width*2, ticks)
    plt.legend(loc=2)
    ax.grid(axis='y', linestyle='-', linewidth=1)
    ax.set_axisbelow(True)

    plt.tight_layout()
    plt.draw()
    
    
def chi_square(a_data, o_data, t_data, expected, headers):
    a_total = [sum(1 for x in a_data.loc[:, col] if x == expected) for col in a_data]
    t_total = [sum(1 for x in t_data.loc[:, col] if x == expected) for col in t_data]
    o_total = [sum(1 for x in o_data.loc[:, col] if x == expected) for col in o_data]
    
    a_total = np.array([x / 2 for x in a_total])
    
    obs = np.array([t_total, o_total])
    chisq, p = stat.chisquare(obs, f_exp=a_total)
    
    chisq = list(chisq)
    p = list(p)
    chisq.insert(0, 'chi-squared')
    p.insert(0, 'p-value')
    
    print(tabulate([chisq, p], headers=[h for h in headers], numalign="right", floatfmt=".3f"))
    print()
    
    
def show_stacked_graph(o_data, t_data):
    t_count = len(t_data)
    t_com = [sum(1 for x in t_data.loc[:, col] if x == 1) for col in t_data]
    t_fail = [sum(1 for x in t_data.loc[:, col] if x == 0) for col in t_data]
    t_part = [t_count - t_com[i] - t_fail[i] for i in range(3)]
            
    o_count = len(o_data)      
    o_com = [sum(1 for x in o_data.loc[:, col] if x == 1) for col in o_data]
    o_fail = [sum(1 for x in o_data.loc[:, col] if x == 0) for col in o_data]
    o_part = [o_count - o_com[i] - o_fail[i] for i in range(3)]
    
    t_com = [x / t_count * 100.0 for x in t_com]
    t_part = [x / t_count * 100.0 for x in t_part]
    t_fail = [x / t_count * 100.0 for x in t_fail]
    
    o_com = [x / o_count * 100.0 for x in o_com]
    o_part = [x / o_count * 100.0 for x in o_part]
    o_fail = [x / o_count * 100.0 for x in o_fail]
    
    index = np.arange(3)
    bar_width = 0.35
    
    fig, ax = plt.subplots()
    plt.bar(index + bar_width, o_com, bar_width, color='#347A2A', 
            label='Complete')
    plt.bar(index + 2 * bar_width, t_com, bar_width, color='#347A2A',)
    plt.bar(index + bar_width, o_part, bar_width, color='#B3C87A', 
            bottom=o_com, label='Partial')
    plt.bar(index + 2 * bar_width, t_part, bar_width, color='#B3C87A', 
            bottom=t_com )
    plt.bar(index + bar_width, o_fail, bar_width, color='#EBE8BE',
            bottom=[i+j for i,j in zip(o_com, o_part)], label='Fail')
    plt.bar(index + 2 * bar_width, t_fail, bar_width, color='#EBE8BE', 
            bottom=[i+j for i,j in zip(t_com, t_part)])
    
    plt.ylabel('% of Participants')
    plt.title('Levels of Success')
    plt.xticks(index + bar_width*2, 
               ('Scope   Tablet \nTask 1', 'Scope   Tablet \nTask 2', 'Scope   Tablet \nTask 3'))
    plt.yticks(np.arange(0, 101, 10))
    plt.legend(loc='upper right', bbox_to_anchor=(1.0, 1.08), fancybox=True, shadow=True)
   
    plt.tight_layout()
    plt.draw()


def main():
    results = pd.read_csv('results.csv')
    for ti in TIME_COLUMNS:
        results[ti] = results[ti].apply(to_seconds)

    results = trim_outliers(results)

    # skew and kurtosis are combined in the normality test
    print('normal distribution for time:')
    print(stat.normaltest(results.loc[:, TIME_COLUMNS])[1])
    #show_distributions(results.loc[:, TIME_COLUMNS])

    print('Time: Overall - testers: ' + str(len(results)))
    basic_stats(results.loc[:, TIME_COLUMNS], ('time 1', 'time 2', 'time 3'))

    oscope_results = results[results.device_coded == 0]
    tablet_results = results[results.device_coded == 1]

    print('Time: Oscilloscope - testers: ' + str(len(oscope_results)))
    basic_stats(oscope_results.loc[:, TIME_COLUMNS], ('time 1', 'time 2', 'time 3'))

    print('Time: Tablet - testers: ' + str(len(tablet_results)))
    basic_stats(tablet_results.loc[:, TIME_COLUMNS], ('time 1', 'time 2', 'time 3'))

    print('t-test independent samples:')
    t_test_ind(oscope_results.loc[:, TIME_COLUMNS], tablet_results.loc[:, TIME_COLUMNS], ('time 1', 'time 2', 'time 3'))

    # anova results
    #print(stat.f_oneway(oscope_results.loc[:, TIME_COLUMNS], tablet_results.loc[:, TIME_COLUMNS]))

    show_bar_graph(oscope_results.loc[:, TIME_COLUMNS], tablet_results.loc[:, TIME_COLUMNS],
                   'Time (sec) to Complete Task',
                   'Tasks',
                   'Mean Time on Task \n(Error bars represents 95% confidence interval)',
                   ('Task 1', 'Task 2', 'Task 3'))
        
    print('Levels of Success: Overall')
    basic_stats(results.loc[:, RESULTS_COLUMNS], ('task 1', 'task 2', 'task 3'))
    
    print('Levels of Success: Oscilloscope')
    basic_stats(oscope_results.loc[:, RESULTS_COLUMNS], ('task 1', 'task 2', 'task 3'))
    
    print('Levels of Success: Tablet')
    basic_stats(tablet_results.loc[:, RESULTS_COLUMNS], ('task 1', 'task 2', 'task 3'))
    
    print('Complete Success')
    chi_square(results.loc[:, RESULTS_COLUMNS], 
               oscope_results.loc[:, RESULTS_COLUMNS], 
               tablet_results.loc[:, RESULTS_COLUMNS],
               1,
               ('Task 1', 'Task 2', 'Task 3'))
    
    show_stacked_graph(oscope_results.loc[:, RESULTS_COLUMNS], 
                       tablet_results.loc[:, RESULTS_COLUMNS])

    plt.show()

main()

