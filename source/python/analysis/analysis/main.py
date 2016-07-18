import pandas as pd
import numpy as np
from scipy import stats as stat
import matplotlib.pyplot as plt
from tabulate import tabulate
import re
import datetime

TIME_COLUMNS = ['time1', 'time2', 'time3']
RESULTS_COLUMNS = ['task1_coded', 'task2_coded', 'task3_coded']
ASSISTANCE_COLUMNS = ['task1_assistance_count', 'task2_assistance_count', 'task3_assistance_count']

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
    bell_curve(data)
    qq_plot(data)


def bell_curve(data):
    fig = 1
    for col in data:
        d = sorted(data.loc[:, col])
        mn = np.mean(d)
        std = np.std(d)
        # fit = stat.norm.pdf(d, mn, std)

        plt.figure(fig)
        fig += 1
        count, bins, ignored = plt.hist(d, 15, normed=True)
        plt.plot(bins, 1 / (std * np.sqrt(2 * np.pi)) * np.exp(- (bins - mn) ** 2 / (2 * std ** 2)),
                 linewidth=2, color='r')
        plt.title(col)
        # plt.plot(d, fit, '-o')
        # plt.hist(d, normed=True)
    plt.draw()


def qq_plot(data):
    fig = 4
    for col in data:
        d = sorted(data.loc[:, col])
        plt.figure(fig)
        fig += 1
        stat.probplot(d, plot=plt)

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
    gmn = list(stat.gmean(data))

    mn.insert(0, 'mean')
    md.insert(0, 'median')
    mo.insert(0, 'mode')
    small.insert(0, 'min')
    big.insert(0, 'max')
    rng.insert(0, 'range')
    std.insert(0, 'std')
    var.insert(0, 'var')
    conf.insert(0, '95% confidence')
    gmn.insert(0, 'geometric mean')

    print(tabulate([small, big, rng, mn, md, mo, gmn, var, std, conf],
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
            color='lightskyblue', yerr=conf1, error_kw=error_config, label='Oscilloscope')
    plt.bar(index + bar_width*2, mn2, bar_width,
            color='lightcoral', yerr=conf2, error_kw=error_config, label='Tablet')

    plt.xlabel(xlabel)
    plt.ylabel(ylabel)
    plt.title(title)
    plt.xticks(index + bar_width*2, ticks)
    plt.legend(loc=2)
    ax.grid(axis='y', linestyle='-', linewidth=1)
    ax.set_axisbelow(True)

    plt.tight_layout()
    plt.draw()
    

''' wrong implementation '''
# def chi_square(a_data, o_data, t_data, expected, headers):
#     a_total = [sum(1 for x in a_data.loc[:, col] if x == expected) for col in a_data]
#     t_total = [sum(1 for x in t_data.loc[:, col] if x == expected) for col in t_data]
#     o_total = [sum(1 for x in o_data.loc[:, col] if x == expected) for col in o_data]
#
#     a_total = np.array([x / 2 for x in a_total])
#
#     obs = np.array([t_total, o_total])
#     chisq, p = stat.chisquare(obs, f_exp=a_total)
#
#     chisq = list(chisq)
#     p = list(p)
#     chisq.insert(0, 'chi-squared')
#     p.insert(0, 'p-value')
#
#     print(tabulate([chisq, p], headers=[h for h in headers], numalign="right", floatfmt=".3f"))
#     print()


def levels_of_success(o_data, t_data):
    t_count = len(t_data)
    t_com = [sum(1 for x in t_data.loc[:, col] if x == 1) for col in t_data]
    t_fail = [sum(1 for x in t_data.loc[:, col] if x == 0) for col in t_data]
    t_part = [t_count - t_com[i] - t_fail[i] for i in range(3)]

    o_count = len(o_data)
    o_com = [sum(1 for x in o_data.loc[:, col] if x == 1) for col in o_data]
    o_fail = [sum(1 for x in o_data.loc[:, col] if x == 0) for col in o_data]
    o_part = [o_count - o_com[i] - o_fail[i] for i in range(3)]

    zipped = np.array(list(zip(t_com, t_part, t_fail, o_com, o_part, o_fail)))
    print_chisquared(zipped, 3)

    
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


def assistance_count(o_data, t_data):
    o_zero = [sum(1 for x in o_data.loc[:, col] if x == 0) for col in o_data]
    o_one = [sum(1 for x in o_data.loc[:, col] if x == 1 ) for col in o_data]
    o_twoup = [sum(1 for x in o_data.loc[:, col] if x > 1) for col in o_data]

    t_zero = [sum(1 for x in t_data.loc[:, col] if x == 0) for col in t_data]
    t_one = [sum(1 for x in t_data.loc[:, col] if x == 1 ) for col in t_data]
    t_twoup = [sum(1 for x in t_data.loc[:, col] if x > 1) for col in t_data]

    zipped = np.array(list(zip(o_zero, o_one, o_twoup, t_zero, t_one, t_twoup)))
    print_chisquared(zipped, 3)

    assistance_pie_chart(o_zero, o_one, o_twoup, t_zero, t_one, t_twoup)


def assistance_pie_chart(o_zero, o_one, o_twoup, t_zero, t_one, t_twoup):
    labels = '0 assistance', '1 assistance', '> 2 assistance'
    colors = ['yellowgreen', 'lightskyblue', 'lightcoral']

    o_data = list(zip(o_zero, o_one, o_twoup))
    t_data = list(zip(t_zero, t_one, t_twoup))

    fig, ax = plt.subplots()

    ax.pie(o_data[0], colors=colors, autopct='%1.1f%%',
           startangle=45, radius=0.3, center=(0, 0), frame=True)
    ax.pie(o_data[1], colors=colors, autopct='%1.1f%%',
           startangle=45, radius=0.3, center=(1, 0), frame=True)
    ax.pie(o_data[2], colors=colors, autopct='%1.1f%%',
           startangle=45, radius=0.3, center=(2, 0), frame=True)
    ax.pie(t_data[0], colors=colors, autopct='%1.1f%%',
           startangle=45, radius=0.3, center=(0, 1), frame=True)
    ax.pie(t_data[1], colors=colors, autopct='%1.1f%%',
           startangle=45, radius=0.3, center=(1, 1), frame=True)
    ax.pie(t_data[2], colors=colors, autopct='%1.1f%%',
           startangle=45, radius=0.3, center=(2, 1), frame=True)

    ax.set_xticks(np.arange(3))
    ax.set_yticks(np.arange(2))
    ax.set_xticklabels(["Task 1", "Task 2", "Task 3"])
    ax.set_yticklabels(["Scope", "Tablet"])
    ax.set_xlim((-0.5, 2.5))
    ax.set_ylim((-0.5, 1.5))
    ax.set_aspect('equal')

    plt.title('Assistance Required')
    plt.legend(labels, loc='upper right', bbox_to_anchor=(1.1, 1.2), fancybox=True, shadow=True)

    plt.draw()


def print_chisquared(zipped, columns):
    chs = ['chi-squared']
    ps = ['p-value']
    dofs = ['degrees of freedom']
    for t in zipped:
        t.resize((2, columns))
        sums = np.sum(t, axis=0)
        for i in range(columns):
            if sums[i] == 0:
                t = np.delete(t, i, 1)
        chi2, p, dof, ex = stat.chi2_contingency(t)
        chs.append(chi2)
        ps.append(p)
        dofs.append(dof)

    print(tabulate([chs, ps, dofs], headers=('Task 1', 'Task 2', 'Task 3'), numalign="right", floatfmt=".3f"))
    print()


def normal_distribution_test(data):
    # skew and kurtosis are combined in the normality test
    print('skew and kurtosis Test:')
    teststat, pvalue = stat.normaltest(data)

    teststat = list(teststat)
    pvalue = list(pvalue)
    teststat.insert(0, 'test stat')
    pvalue.insert(0, 'p-value')

    print(tabulate([teststat, pvalue],
                   headers=('time 1', 'time 2', 'time 3'), numalign="right", floatfmt=".2f"))

    print()
    print('Shapiro-Wilk Test:')
    ws = ['w-value']
    ps = ['p-value']
    for col in data:
        w, p = stat.shapiro(data.loc[:, col])
        ws.append(w)
        ps.append(p)
    print(tabulate([ws, ps],
                   headers=('time 1', 'time 2', 'time 3'), numalign="right", floatfmt=".2f"))
    print('----------------------------------------------------------------------\n')


def main():
    results = pd.read_csv('results.csv')
    for ti in TIME_COLUMNS:
        results[ti] = results[ti].apply(to_seconds)

    results = trim_outliers(results)

    print('normal distribution for time:')
    normal_distribution_test(results.loc[:, TIME_COLUMNS])

    show_distributions(results.loc[:, TIME_COLUMNS])

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

    show_bar_graph(oscope_results.loc[:, TIME_COLUMNS], tablet_results.loc[:, TIME_COLUMNS],
                   'Time (sec) to Complete Task',
                   'Tasks',
                   'Mean Time on Task \n(Error bars represents 95% confidence interval)',
                   ('Task 1', 'Task 2', 'Task 3'))

    print('----------------------------------------------------------------------\n')
    # print('Levels of Success: Overall')
    # basic_stats(results.loc[:, RESULTS_COLUMNS], ('task 1', 'task 2', 'task 3'))
    #
    # print('Levels of Success: Oscilloscope')
    # basic_stats(oscope_results.loc[:, RESULTS_COLUMNS], ('task 1', 'task 2', 'task 3'))
    #
    # print('Levels of Success: Tablet')
    # basic_stats(tablet_results.loc[:, RESULTS_COLUMNS], ('task 1', 'task 2', 'task 3'))
    
    print('Level of Success')
    levels_of_success(oscope_results.loc[:, RESULTS_COLUMNS], tablet_results.loc[:, RESULTS_COLUMNS])
    
    show_stacked_graph(oscope_results.loc[:, RESULTS_COLUMNS], 
                       tablet_results.loc[:, RESULTS_COLUMNS])

    print('----------------------------------------------------------------------\n')
    print('Assistance Count')
    assistance_count(oscope_results.loc[:, ASSISTANCE_COLUMNS], tablet_results.loc[:, ASSISTANCE_COLUMNS])


    plt.show()

main()

