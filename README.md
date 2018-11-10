## 信息检索大作业 Part 1

### 代码文件

共有4个文件，其中`Main`是程序入口，`Config`是一些全局配置，`Index`用于建立索引，`Search`用于执行搜索。

### 运行方法

```
mvn package
```

`target/`目录下生成可执行jar包`ir-1.0-SNAPSHOT-jar-with-dependencies.jar`，可将其移动到任意路径中。下文在该jar包的路径中做说明。

#### 建立索引

假设`./data/`目录中有原始数据`CNKI_journal_v2.txt`，要建立索引，运行：

```
java -jar ir-1.0-SNAPSHOT-jar-with-dependencies.jar Index -i ./data/CNKI_journal_v2.txt -o ./index_ena_smart
```

这会在`./index_ena_smart/`目录下生成索引，英文默认使用分词器`EnglishAnalyzer`，中文默认使用`SmartChineseAnalyzer`.

再运行：

```
java -jar ir-1.0-SNAPSHOT-jar-with-dependencies.jar Index \
-i ./data/CNKI_journal_v2.txt -o ./index_std_std \
--en-analyzer org.apache.lucene.analysis.standard.StandardAnalyzer \
--cn-analyzer org.apache.lucene.analysis.standard.StandardAnalyzer
```

这会在`./index_std_std/`目录下生成索引。通过Java反射机制，中英文都使用了分词器`StandardAnalyzer`.

#### 搜索

有了上面的两组索引数据，可以执行搜索：

```
java -jar ir-1.0-SNAPSHOT-jar-with-dependencies.jar Search -i ./index_ena_smart
```

然后程序会等待从标准输入中读取查询字符串。回车后显示搜索结果。这是从比较好的中英文分词索引中搜索。

要在中英文分词器都是`StandardAnalyzer`的索引中搜索，执行：

```
java -jar ir-1.0-SNAPSHOT-jar-with-dependencies.jar Search -i ./index_std_std \
--en-analyzer org.apache.lucene.analysis.standard.StandardAnalyzer \
--cn-analyzer org.apache.lucene.analysis.standard.StandardAnalyzer \
--q-analyzer org.apache.lucene.analysis.standard.StandardAnalyzer
```

这里指定了3个非默认的Analyzer，原因见 “简要实现步骤”-“搜索” 中的说明。

#### Usage

```
usage: Index
 -c,--cn-analyzer <arg>   Chinese analyzer
 -e,--en-analyzer <arg>   English analyzer
 -i,--input <arg>         input data path
 -o,--output <arg>        output index path
```

```
usage: Search
 -a,--q-analyzer <arg>    query analyzer
 -c,--cn-analyzer <arg>   Chinese analyzer
 -e,--en-analyzer <arg>   English analyzer
 -i,--index <arg>         index path
 -n,--top-n <arg>         show top n results
```

### 简要实现步骤

#### 建立索引

步骤简要叙述如下：

1. 指定原始数据路径、生成索引目标路径、分词器 (Analyzer)
2. 构造`IndexWriter`对象`iWriter`
3. 构造`Document`对象`doc`，按行读取原始数据，每行解析成一个`Field`，添加进`doc`；每遇到一行`<REC>`，则用`iWriter`将`doc`加入索引集合，重新构造一个空`doc`
4. 关闭`iWriter`和目标路径等

分词器原本只能有一个，但我觉得中英文应该有所区别，所以用了`PerFieldAnalyzerWrapper`，来实现对不同的Field切换中英两种分词器。

#### 搜索

步骤简要叙述如下：

1. 指定索引存储路径、query分词器
2. 构造`IndexSearcher`对象`iSearcher`
3. 构造`MultiFieldQueryParser`对象`parser`
4. 用`parser`解析查询字符串，并交给`iSearcher`搜索，得到排序的结果文档列表
5. 打印结果文档

在打印结果这一步骤，我还使用了lucene的高亮功能，以截取出搜索匹配的段落。原本的搜索过程只需要指定查询字符串的分词器，但高亮功能还需要提供原文的分词器，将存储的原文分词，以便与查询字符串的分词相对比，找到应当高亮的地方。复用了建立索引时的`PerFieldAnalyzerWrapper`，故命令行可以分别提供中英分词器。

**注意，搜索时指定的查询分词器、原文分词器，应当与建立对应索引时使用的分词器相匹配。**假如建立索引时使用了`StandardAnalyzer`，会把中文按字切分；然而搜索时使用了`SmartChineseAnalyzer`，会把查询字符串按词切分。这样，切分后的查询词只要长一个字以上，就不可能在倒排索引中找到匹配。

### 实例结果分析

分别在`index_std_std`和`index_ena_smart`上查询**“共产国际”**，结果如下：

-  `index_std_std`上的结果：

```
======== Results ========
Title: 中<B>国</B>知识<B>产</B>权<B>国</B><B>际</B>保护的人权视野
Authors: 刘茂林;刘永
Publisher: 安徽农业大学学报(社会科学版)
Keywords: 知识<B>产</B>权<B>国</B><B>际</B>保护;;人权视野;;人权要求
Abstract: 力和呼吁;但由于诸努力仍停留在知识<B>产</B>权私权保护的视野之下,故不足以真正建立公正、开放的知识<B>产</B>权<B>国</B><B>际</B>保护秩序,惟有清楚地认识到知识<B>产</B>权是知识经济时代本<B>共</B>同体人权视野下的人权要求,才能构建出符合我<B>国</B>宪政实情的知识<B>产</B>权<B>国</B><B>际</B>保护秩序。
Year: 2010
Issue: 04
=========================
Title: <B>国</B><B>际</B>市场对我<B>国</B>棉花生<B>产</B>的影响及其对策
Authors: 郑芝奖
Publisher: 安徽农学通报
Keywords: 棉花生<B>产</B>;;<B>国</B><B>际</B>;;市场　WTO
Abstract: 本文论述了<B>国</B><B>际</B>市场现状及其对我<B>国</B>棉花生<B>产</B>的影响 ,提出了发展我<B>国</B>棉花生<B>产</B>的对策
Year: 2000
Issue: 03
=========================
Title: 浅析<B>国</B><B>际</B>非政府组织在<B>国</B><B>际</B>法中的地位
Authors: 尹玮
Publisher: 安徽农业大学学报(社会科学版)
Keywords: <B>国</B><B>际</B>非政府组织;;全球化;;<B>国</B><B>际</B>地位
Abstract: 随着全球化的发展,<B>国</B><B>际</B>非政府组织的影响力越来越大,但对于其在<B>国</B><B>际</B>法上的地位问题还存在着争议。本文通过对<B>国</B><B>际</B>非政府组织的定义、发展历程和兴起原因、<B>国</B><B>际</B>法上的地位三个方面进行分析,认为<B>国</B><B>际</B>非政府组织在一定范围和条件是可以成为<B>国</B><B>际</B>法主体的,应给予这些<B>国</B><B>际</B>非政府组织<B>国</B><B>际</B>法中的地位。
Year: 2007
Issue: 04
=========================
Title: 从伊拉克战争看<B>国</B><B>际</B>法对大<B>国</B>的制约
Authors: 赵丹;陈鸿燕
Publisher: 安徽农学通报
Keywords: <B>国</B><B>际</B>法;;制约;;<B>国</B><B>际</B>关系
Abstract: 伊拉克战争是在美<B>国</B>主导下一场践踏<B>国</B><B>际</B>法的战争,反映了<B>国</B><B>际</B>关系中霸权主义和强权政治仍然强大的现实,折射出<B>国</B><B>际</B>法的脆弱性。但同时,作为制约大<B>国</B>霸权维护<B>国</B><B>际</B>秩序的重要因素,<B>国</B><B>际</B>法的权威性也是不容置疑的,大
Year: 2007
Issue: 19
=========================
Title: 亚洲4<B>国</B>农村公<B>共</B><B>产</B>品供给经验分析及对我<B>国</B>的启示
Authors: 闫春香;侯立白
Publisher: 安徽农学通报(上半月刊)
Keywords: 亚洲4<B>国</B>;;农村公<B>共</B><B>产</B>品;;供给;;经验;;启示
Abstract: 概述了农村公<B>共</B><B>产</B>品的内涵,介绍并分析了亚洲4<B>国</B>即日本、韩<B>国</B>、泰<B>国</B>和印度的农村公<B>共</B><B>产</B>品供给情况,总结上述4<B>国</B>的成功经验,有助于为我<B>国</B>农村公<B>共</B><B>产</B>品的供给提供有益参考和启迪。
Year: 2011
Issue: 09
=========================
Title: 中<B>国</B><B>共</B><B>产</B>党与民间组织关系的历史评述
Authors: 闫东
Publisher: 安徽农业大学学报(社会科学版)
Keywords: 中<B>国</B><B>共</B><B>产</B>党;;民间组织;;关系
Abstract: 由于历史环境的不同,中<B>国</B><B>共</B><B>产</B>党与民间组织的关系大致经历<B>共</B>生型、隶属型与依附型领导关系等几个阶段。在此历史经验教训的基础上,党与民间组织的关系应构建为合作型领导关系。
Year: 2010
Issue: 01
=========================
Title: 论我<B>国</B>软件企业<B>国</B><B>际</B>化战略
Authors: 李俊
Publisher: 安徽科技
Keywords: 软件企业;;<B>国</B><B>际</B>化;;战略
Abstract: 本文对我<B>国</B>软件企业的<B>国</B><B>际</B>化状况进行了概述,并对我<B>国</B>软件企业实施<B>国</B><B>际</B>化战略的可行性与必要性进行了分析,提出了推动我<B>国</B>软件企业<B>国</B><B>际</B>化的战略建议。
Year: 2007
Issue: 03
=========================
Title: 论邓小平<B>国</B><B>际</B>战略思想
Authors: 倪薇
Publisher: 安徽农业大学学报(社会科学版)
Keywords: 邓小平;;<B>国</B><B>际</B>战略;;中<B>国</B>外交
Abstract: 和平与发展的时代主题是邓小平<B>国</B><B>际</B>战略思想形成的主要依据。坚持独立自主,争取和维护世界和平,积极推动建立<B>国</B><B>际</B>政治经济新秩序,努力成为多极世界格局中的重要一极,在<B>国</B><B>际</B>事务中发挥积极作用是邓小平<B>国</B><B>际</B>战略思
Year: 2002
Issue: 02
=========================
Title: 浅谈<B>国</B><B>际</B>工程承包中的保函
Authors: 吴康
Publisher: 安徽建筑
Keywords: <B>国</B><B>际</B>工程;;保函
Abstract: 在<B>国</B><B>际</B>工程承包业务中,保函自始至终都在伴随着<B>国</B><B>际</B>工程项目。熟悉保函运作有助于对外工程承包企业参与<B>国</B><B>际</B>工程项目竟标及实施,并最大限度地避免工程亏损。
Year: 2005
Issue: 05
=========================
Title: <B>国</B><B>际</B>新闻报道的<B>国</B>家民族观
Authors: 郑汉江
Publisher: 安徽农业大学学报(社会科学版)
Keywords: <B>国</B>家民族观;;<B>国</B><B>际</B>新闻;;报道
Abstract: 世界上没有绝对的新闻自由和客观报道,新闻报道服从、服务于<B>国</B>家民族利益,有鲜明的<B>国</B>家民族性。在<B>国</B><B>际</B>传媒市场新闻霸权依然存在的今天,坚持正确的<B>国</B>家民族观显得尤为重要。在<B>国</B><B>际</B>新闻报道中,必须主动出击,积极应对西方传媒对全球新闻的话语权垄断;要有大局观念,把握好度,把可读性与<B>国</B>家民族观有机结合起来。
Year: 2006
Issue: 06
=========================
```

结果中的`<B>...</B>`是高亮器自动添加的HTML加粗语法，虽然在命令行中没有效果，但可以用来看出索引内部的分词情况。可以看到，由于使用了`StandardAnalyzer`，中文被按字切割了。所以，虽然每条结果都有“共”“产”“国”“际”四个字的一或多个，但并没有整个词的出现——它被淹没在无数结果的海洋中了。

- 在`index_ena_smart`上的结果：

```
======== Results ========
Title: 王稼祥与延安整风
Authors: 解莉
Publisher: 安徽农业大学学报(社会科学版)
Keywords: 王稼祥;;延安整风运动;;<B>共产国际</B>;;中共六届六中全会
Abstract: 作为中共第一代卓越的领导人之一,王稼祥为延安整风运动的顺利开展和胜利完成作出了突出贡献。早在党的六届六中全会上,王稼祥完整正确地传达了<B>共产国际</B>的指示,因此有力破除了王明的右倾机会主义错误,为延安整风
Year: 2010
Issue: 01
=========================
```

只有一条结果，但非常精准！这是因为`SmartChineseAnalyzer`识别“共产国际”为一个词，倒排索引准确地找到了这个词唯一对应的文档。

程序支持英文索引，结果中会显示匹配的英文Field片段。但由于找不到英文的好例子，这里没有展示结果。