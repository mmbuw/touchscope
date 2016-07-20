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

QUESTIONNAIRE_COLUMNS = ['after pre-test', 'after post-test']

BACKGROUND_COLUMNS = ['What is your occupation? ',
                      'In which field? (engineering, arts, architecture, etc)',
                      'What is your age?',
                      'What is your gender?']


def to_seconds(t):
    m, s = re.split(':', t)
    seconds = int(datetime.timedelta(minutes=int(m), seconds=int(s)).total_seconds())
    return seconds


def trim_outliers(data, columns):
    # trim outliers that are more than three standard deviations above the mean
    # (Measuring the User Experience p.78)
    
    for col in columns:
        mn = np.mean(data.loc[:, col])
        std = np.std(data.loc[:, col])
        # data.loc[data[t] > (mn + std * 3), t] = mn # fill in with mean
        data.loc[data[col] > (mn + std * 3), col] = np.nan
    before = len(data)
    data = data.dropna(how='any')
    after = len(data)
    print(str(before - after) + ' outliers removed\n')
    return data


def show_distributions(data, titles):
    # bell_curve(data, titles)
    qq_plot(data, titles)


def bell_curve(data, titles):
    for col, title in zip(data, titles):
        d = sorted(data.loc[:, col])
        mn = np.mean(d)
        std = np.std(d)
        # fit = stat.norm.pdf(d, mn, std)

        plt.subplots()
        count, bins, ignored = plt.hist(d, 15, normed=True)
        plt.plot(bins, 1 / (std * np.sqrt(2 * np.pi)) * np.exp(- (bins - mn) ** 2 / (2 * std ** 2)),
                 linewidth=2, color='r')
        plt.title(title)
        # plt.plot(d, fit, '-o')
        # plt.hist(d, normed=True)
        plt.draw()


def qq_plot(data, titles):
    for col, title in zip(data, titles):
        d = sorted(data.loc[:, col])
        plt.subplots()
        stat.probplot(d, plot=plt)
        plt.title(title)
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


def t_test_ind(o_data, t_data, headers, tailed=2):
    t, p = list(stat.ttest_ind(o_data, t_data, equal_var=False))
    t = list(t)
    p = list(p)
    if tailed == 1:
        p = [x / 2 for x in p]
    t.insert(0, 't')  # (' + str(len(results) - 1) + ')')
    p.insert(0, 'p-value')
    print(tabulate([t, p], headers=[h for h in headers], numalign="right", floatfmt=".3f"))
    print()


def show_bar_graph(o_data, t_data, ylabel, xlabel, title, ticks):
    mn1 = list(np.mean(o_data))
    conf1 = confidence_interval(o_data)

    mn2 = list(np.mean(t_data))
    conf2 = confidence_interval(t_data)

    index = np.arange(len(ticks))
    bar_width = 0.20
    error_config = {'ecolor': '0'}

    fig, ax = plt.subplots()
    plt.bar(index + bar_width, mn1, bar_width,
            color='lightskyblue', yerr=conf1, error_kw=error_config, label='Oscilloscope')
    plt.bar(index + bar_width*2, mn2, bar_width,
            color='lightcoral', yerr=conf2, error_kw=error_config, label='Tablet')

    if len(xlabel) > 0:
        plt.xlabel(xlabel)
    plt.ylabel(ylabel)
    plt.title(title)
    plt.xticks(index + bar_width*2, ticks)
    plt.legend(loc='best')
    ax.grid(axis='y', linestyle='-', linewidth=1)
    ax.set_axisbelow(True)
    ax.set_xlim((0.0, len(ticks)))

    plt.tight_layout()
    plt.draw()


def show_box_plot(o_data, t_data, ylabel, titles):
    for col, title in zip(o_data, titles):
        fig, ax = plt.subplots()
        plt.boxplot([o_data.loc[:, col], t_data.loc[:, col]])
        ax.set_xticklabels(["Scope", "Tablet"])
        plt.ylabel(ylabel)
        plt.title(title)
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

    for col, i in zip(o_data, range(3)):
        fail = ['Failed', o_fail[i], t_fail[i]]
        part = ['Partial', o_part[i], t_part[i]]
        success = ['Success', o_com[i], t_com[i]]
        print('Task {0}'.format(i+1))
        print(tabulate([success, part, fail], headers=['scope', 'tablet'], numalign="right", floatfmt=".3f"))
        print()

    # zipped = np.array(list(zip(t_com, t_part, t_fail, o_com, o_part, o_fail)))
    # print_chisquared(zipped, 3)

    print_mannwhitneyu(o_data, t_data)

    
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
    plt.bar(index + 2 * bar_width, t_com, bar_width, color='#347A2A')
    plt.bar(index + bar_width, o_part, bar_width, color='#B3C87A', 
            bottom=o_com, label='Partial')
    plt.bar(index + 2 * bar_width, t_part, bar_width, color='#B3C87A',
            bottom=t_com)
    plt.bar(index + bar_width, o_fail, bar_width, color='#EBE8BE',
            bottom=[i+j for i, j in zip(o_com, o_part)], label='Fail')
    plt.bar(index + 2 * bar_width, t_fail, bar_width, color='#EBE8BE', 
            bottom=[i+j for i, j in zip(t_com, t_part)])
    
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
    o_one = [sum(1 for x in o_data.loc[:, col] if x == 1) for col in o_data]
    o_twoup = [sum(1 for x in o_data.loc[:, col] if x > 1) for col in o_data]

    t_zero = [sum(1 for x in t_data.loc[:, col] if x == 0) for col in t_data]
    t_one = [sum(1 for x in t_data.loc[:, col] if x == 1) for col in t_data]
    t_twoup = [sum(1 for x in t_data.loc[:, col] if x > 1) for col in t_data]

    for col, i in zip(o_data, range(3)):
        zero = ['0 times', o_zero[i], t_zero[i]]
        one = ['1 time', o_one[i], t_one[i]]
        twoup = ['>= 2 times', o_twoup[i], t_twoup[i]]
        print('Task {0}'.format(i + 1))
        print(tabulate([zero, one, twoup], headers=['scope', 'tablet'], numalign="right", floatfmt=".3f"))
        print()

    # zipped = np.array(list(zip(o_zero, o_one, o_twoup, t_zero, t_one, t_twoup)))
    # print_chisquared(zipped, 3)

    print_mannwhitneyu(o_data, t_data)

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

    plt.title('Assistance Count')
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


def print_mannwhitneyu(o_data, t_data):
    wvalues = ['w-value']
    pvalues = ['p-values']
    for col in o_data:
        wvalue, pvalue = stat.mannwhitneyu(o_data.loc[:, col], t_data.loc[:, col])
        wvalues.append(wvalue)
        pvalues.append(pvalue * 2) # to make two-sided

    print('Mann-Whitney rank test')
    print(tabulate([wvalues, pvalues],
                   headers=('Task 1', 'Task 2', 'Task 3'), numalign="right", floatfmt=".2f"))
    print()


def normal_distribution_test(data, headers):
    # skew and kurtosis are combined in the normality test
    # teststat, pvalue = stat.normaltest(data)

    print('Skew Test:')
    skew, pvalue = stat.skewtest(data)
    skew = list(skew)
    pvalue = list(pvalue)
    skew.insert(0, 'skew stat')
    pvalue.insert(0, 'p-value')
    print(tabulate([skew, pvalue],
                   headers=(h for h in headers), numalign="right", floatfmt=".2f"))

    print('\nKurtosis Test:')
    kurt, pvalue = stat.kurtosistest(data)
    kurt = list(kurt)
    pvalue = list(pvalue)
    kurt.insert(0, 'kurtosis stat')
    pvalue.insert(0, 'p-value')
    print(tabulate([kurt, pvalue],
                   headers=(h for h in headers), numalign="right", floatfmt=".2f"))

    print('\nShapiro-Wilk Test:')
    ws = ['w-value']
    ps = ['p-value']
    for col in data:
        w, p = stat.shapiro(data.loc[:, col])
        ws.append(w)
        ps.append(p)
    print(tabulate([ws, ps],
                   headers=(h for h in headers), numalign="right", floatfmt=".2f"))
    print('----------------------------------------------------------------------\n')


def frequencies(o_data, t_data):
    for col in o_data:
        o = pd.Series(list(o_data.loc[:, col]))
        t = pd.Series(list(t_data.loc[:, col]))
        o_count = o.value_counts()
        t_count = t.value_counts()

        join = pd.DataFrame({'scope': o_count, 'tablet': t_count})
        join = join.fillna(0)
        print(col)
        print(join)

    print()


def results_data():
    print('######################################################################')
    print('# Time, Levels of Success, Assistance Data')
    print('######################################################################\n')

    results = pd.read_csv('results.csv')
    for ti in TIME_COLUMNS:
        results[ti] = results[ti].apply(to_seconds)

    results = trim_outliers(results, TIME_COLUMNS)

    print('normal distribution for time:')
    normal_distribution_test(results.loc[:, TIME_COLUMNS], ['time 1', 'time 2', 'time 3'])

    show_distributions(results.loc[:, TIME_COLUMNS],
                       ['Task 1 - Time on Task', 'Task 2 - Time on Task', 'Task 3 - Time on Task'])

    print('Time: Overall - testers: ' + str(len(results)))
    basic_stats(results.loc[:, TIME_COLUMNS], ('time 1', 'time 2', 'time 3'))

    oscope_results = results[results.device_coded == 0]
    tablet_results = results[results.device_coded == 1]

    print('Time: Oscilloscope - testers: ' + str(len(oscope_results)))
    basic_stats(oscope_results.loc[:, TIME_COLUMNS], ('time 1', 'time 2', 'time 3'))

    print('Time: Tablet - testers: ' + str(len(tablet_results)))
    basic_stats(tablet_results.loc[:, TIME_COLUMNS], ('time 1', 'time 2', 'time 3'))

    print('t-test independent samples: 2-tailed')
    t_test_ind(oscope_results.loc[:, TIME_COLUMNS], tablet_results.loc[:, TIME_COLUMNS], ('time 1', 'time 2', 'time 3'))

    show_bar_graph(oscope_results.loc[:, TIME_COLUMNS], tablet_results.loc[:, TIME_COLUMNS],
                   'Time (sec) to Complete Task',
                   '',
                   'Mean Time on Task \n(Error bars represents 95% confidence interval)',
                   ('Task 1', 'Task 2', 'Task 3'))

    show_box_plot(oscope_results.loc[:, TIME_COLUMNS], tablet_results.loc[:, TIME_COLUMNS],
                  'Time (sec) to Complete Task',
                  ['Task 1 - Time on Task', 'Task 2 - Time on Task', 'Task 3 - Time on Task'])

    print('----------------------------------------------------------------------\n')
    print('Level of Success')
    levels_of_success(oscope_results.loc[:, RESULTS_COLUMNS], tablet_results.loc[:, RESULTS_COLUMNS])
    show_stacked_graph(oscope_results.loc[:, RESULTS_COLUMNS], 
                       tablet_results.loc[:, RESULTS_COLUMNS])

    print('----------------------------------------------------------------------\n')
    print('Assistance Count')
    assistance_count(oscope_results.loc[:, ASSISTANCE_COLUMNS], tablet_results.loc[:, ASSISTANCE_COLUMNS])


def questionnaire_data():
    print('######################################################################')
    print('# Questionnaire Data')
    print('######################################################################\n')

    results = pd.read_csv('questionnaire_results.csv')

    print('normal distribution for questionnaire:')
    normal_distribution_test(results.loc[:, QUESTIONNAIRE_COLUMNS], ['After Learning Phase', 'After Test Phase'])

    show_distributions(results.loc[:, QUESTIONNAIRE_COLUMNS],
                       ['After Learning Phase - SUS Scores', 'After Test Phase - SUS Scores'])

    print('Questionnaires: Overall - testers: ' + str(len(results)))
    basic_stats(results.loc[:, QUESTIONNAIRE_COLUMNS], ('After Learning Phase', 'After Test Phase'))

    oscope_results = results[results.device_coded == 0]
    tablet_results = results[results.device_coded == 1]

    print('Questionnaires: Oscilloscope - testers: ' + str(len(oscope_results)))
    basic_stats(oscope_results.loc[:, QUESTIONNAIRE_COLUMNS], ('After Learning Phase', 'After Test Phase'))

    print('Questionnaires: Tablet - testers: ' + str(len(tablet_results)))
    basic_stats(tablet_results.loc[:, QUESTIONNAIRE_COLUMNS], ('After Learning Phase', 'After Test Phase'))

    print('t-test independent samples : 1-tailed')
    t_test_ind(oscope_results.loc[:, QUESTIONNAIRE_COLUMNS],
               tablet_results.loc[:, QUESTIONNAIRE_COLUMNS],
               ('After Learning Phase', 'After Test Phase'),
               tailed=1)

    show_bar_graph(oscope_results.loc[:, QUESTIONNAIRE_COLUMNS], tablet_results.loc[:, QUESTIONNAIRE_COLUMNS],
                   'SUS Score',
                   'Questionnaires',
                   'Mean SUS Score \n(Error bars represents 95% confidence interval)',
                   ('After Learning Phase', 'After Test Phase'))

    show_box_plot(oscope_results.loc[:, QUESTIONNAIRE_COLUMNS], tablet_results.loc[:, QUESTIONNAIRE_COLUMNS],
                  'SUS Score',
                  ('SUS Scores - After Learning Phase', 'SUS Scores - After Test Phase'))


def background_data():
    print('######################################################################')
    print('# Background Data')
    print('######################################################################\n')
    results = pd.read_csv('background.csv')
    oscope_results = results[results.device_coded == 0]
    tablet_results = results[results.device_coded == 1]
    frequencies(oscope_results.loc[:, BACKGROUND_COLUMNS], tablet_results.loc[:, BACKGROUND_COLUMNS])


def main():
    results_data()
    questionnaire_data()
    background_data()
    plt.show()


main()
