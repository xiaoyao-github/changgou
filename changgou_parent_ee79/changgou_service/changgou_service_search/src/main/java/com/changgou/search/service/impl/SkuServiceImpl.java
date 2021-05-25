package com.changgou.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.changgou.goods.feign.SkuFeign;
import com.changgou.goods.pojo.Sku;
import com.changgou.search.dao.SkuEsMapper;
import com.changgou.search.pojo.SkuInfo;
import com.changgou.search.service.SkuService;
import entity.Result;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author Steven
 * @version 1.0
 * @description com.changgou.search.service.impl
 * @date 2019-12-25
 */
@Service
public class SkuServiceImpl implements SkuService {

    @Autowired
    private SkuEsMapper skuEsMapper;
    @Autowired
    private SkuFeign skuFeign;
    @Autowired
    private ElasticsearchTemplate esTemplate;

    @Override
    public void importSku() {
        //1、调用skuFeign，查询所有sku列表
        List<Sku> skuList = skuFeign.findByStatus("1").getData();//只查询正常状态的sku列表
        if(skuList != null && skuList.size() > 0){
            List<SkuInfo> skuInfoList = new ArrayList<>();
            SkuInfo skuInfo = null;
            //2、组装SkuInfo列表
            for (Sku sku : skuList) {
                //把sku转成json串，
                String skuJSON = JSON.toJSONString(sku);
                //再转回为SkuInfo，两个对象的属性名要一样
                skuInfo = JSON.parseObject(skuJSON, SkuInfo.class);

                //把spec规格属性转成Map,存到嵌套域中,此刻es会自动创建多个嵌套域(key域名,value域的值)
                Map specMap = JSON.parseObject(skuInfo.getSpec(), Map.class);
                skuInfo.setSpecMap(specMap);

                //不推荐使用一个个导入
                //skuEsMapper.save(skuInfo);
                //保存sku列表
                skuInfoList.add(skuInfo);
            }
            //3、批量导入数据 (推荐使用)
            skuEsMapper.saveAll(skuInfoList);
        }

    }

    @Override
    public Map search(Map<String, String> searchMap) {
        Map<String, Object> map = new HashMap<>();
        //构建基本查询条件
        NativeSearchQueryBuilder builder = builderBasicQuery(searchMap);
        //1、根据查询条件，查询商品列表
        searchList(map, builder);
        /*//2、根据查询条件，分组查询分类列表
        searchCategoryList(map, builder);
        //3、根据查询条件，分组查询品牌列表
        searchBrandList(map, builder);
        //4、根据查询条件，分组查询规格列表
        searchSpecMap(map, builder);*/

        //2、根据查询条件，分组查询分类、品牌、规格列表
        searchGroup(map, builder);

        return map;
    }

    /**
     * 聚合查询-分组
     * 根据查询条件，分组查询分类、品牌、规格列表
     * @param map 结果集
     * @param builder 查询条件构建器
     */
    private void searchGroup(Map<String, Object> map, NativeSearchQueryBuilder builder) {

        //设置分类聚合条件
        //1.设置分组域名-termsAggregationBuilder = AggregationBuilders.terms(别名).field(域名);
        TermsAggregationBuilder categoryAggregationBuilder = AggregationBuilders.terms("group_category").field("categoryName");
        //2.添加分组查询参数-builder.addAggregation(termsAggregationBuilder)
        builder.addAggregation(categoryAggregationBuilder);

        //设置品牌聚合条件
        //1.设置分组域名-termsAggregationBuilder = AggregationBuilders.terms(别名).field(域名);
        TermsAggregationBuilder brandAggregationBuilder = AggregationBuilders.terms("group_brand").field("brandName");
        //2.添加分组查询参数-builder.addAggregation(termsAggregationBuilder)
        builder.addAggregation(brandAggregationBuilder);

        //设置规格聚合条件
        //1.设置分组域名-termsAggregationBuilder = AggregationBuilders.terms(别名).field(域名);
        //聚合查询中，默认只会搜索前10条数据，可以通过size(聚合记录数量)，查询更多的数据
        TermsAggregationBuilder specAggregationBuilder = AggregationBuilders.terms("group_spec").field("spec.keyword").size(10000000);
        //2.添加分组查询参数-builder.addAggregation(termsAggregationBuilder)
        builder.addAggregation(specAggregationBuilder);

        //三个聚合条件，只查询一次索引库
        //3.执行搜索-esTemplate.queryForPage(builder.build(), SkuInfo.class)
        AggregatedPage<SkuInfo> page = esTemplate.queryForPage(builder.build(), SkuInfo.class);
        //4.获取所有分组查询结果集-page.getAggregations()
        Aggregations aggregations = page.getAggregations();

        //提取分类聚合数据
        //5.提取分组结果数据-stringTerms = aggregations.get(填入刚才查询时的别名)
        StringTerms categoryTerms = aggregations.get("group_category");
        //6.定义分类名字列表-categoryList = new ArrayList<String>()
        List<String> categoryList = new ArrayList<String>();
        //7.遍历读取分组查询结果-stringTerms.getBuckets().for
        for (StringTerms.Bucket bucket : categoryTerms.getBuckets()) {
            //7.1获取分类名字，并将分类名字存入到集合中-bucket.getKeyAsString()
            categoryList.add(bucket.getKeyAsString());
        }
        //8.返回分类数据列表-map.put("categoryList", categoryList)
        map.put("categoryList", categoryList);

        //提取品牌聚合数据
        //5.提取分组结果数据-stringTerms = aggregations.get(填入刚才查询时的别名)
        StringTerms brandTerms = aggregations.get("group_brand");
        //6.定义分类名字列表-categoryList = new ArrayList<String>()
        List<String> brandList = new ArrayList<String>();
        //7.遍历读取分组查询结果-stringTerms.getBuckets().for
        for (StringTerms.Bucket bucket : brandTerms.getBuckets()) {
            //7.1获取分类名字，并将分类名字存入到集合中-bucket.getKeyAsString()
            brandList.add(bucket.getKeyAsString());
        }
        //8.返回分类数据列表-map.put("categoryList", categoryList)
        map.put("brandList", brandList);

        //提取规格聚合数据
        //5.提取分组结果数据-stringTerms = aggregations.get(填入刚才查询时的别名)
        StringTerms specTerms = aggregations.get("group_spec");
        //6.定义分类名字列表-categoryList = new ArrayList<String>()
        List<String> specList = new ArrayList<String>();
        //7.遍历读取分组查询结果-stringTerms.getBuckets().for
        for (StringTerms.Bucket bucket : specTerms.getBuckets()) {
            //7.1获取分类名字，并将分类名字存入到集合中-bucket.getKeyAsString()
            specList.add(bucket.getKeyAsString());
        }
        Map<String, Set<String>> specMap = new HashMap<String, Set<String>>();
        //把spec转Map
        for (String json : specList) {
            //{"电视音响效果":"小影院","电视屏幕尺寸":"20英寸","尺码":"165"}
            Map<String,String> tempMap = JSON.parseObject(json, Map.class);
            for (String key : tempMap.keySet()) {
                Set<String> tempSet = specMap.get(key);
                //第一次运算，Set是为空的
                if(tempSet == null || tempSet.size() < 1){
                    tempSet = new HashSet<String>();
                }
                //追加元索
                tempSet.add(tempMap.get(key));
                //记录元索
                specMap.put(key, tempSet);
            }
        }
        //把规格返回
        map.put("specMap", specMap);
    }

    /**
     * 聚合查询-分组
     * 根据查询条件，分组查询规格列表
     * @param map 结果集
     * @param builder 查询条件构建器
     */
    private void searchSpecMap(Map<String, Object> map, NativeSearchQueryBuilder builder) {
        //1.设置分组域名-termsAggregationBuilder = AggregationBuilders.terms(别名).field(域名);
        //聚合查询中，默认只会搜索前10条数据，可以通过size(聚合记录数量)，查询更多的数据
        TermsAggregationBuilder termsAggregationBuilder = AggregationBuilders.terms("group_spec").field("spec.keyword").size(10000000);
        //2.添加分组查询参数-builder.addAggregation(termsAggregationBuilder)
        builder.addAggregation(termsAggregationBuilder);
        //3.执行搜索-esTemplate.queryForPage(builder.build(), SkuInfo.class)
        AggregatedPage<SkuInfo> page = esTemplate.queryForPage(builder.build(), SkuInfo.class);
        //4.获取所有分组查询结果集-page.getAggregations()
        Aggregations aggregations = page.getAggregations();
        //5.提取分组结果数据-stringTerms = aggregations.get(填入刚才查询时的别名)
        StringTerms stringTerms = aggregations.get("group_spec");
        //6.定义分类名字列表-categoryList = new ArrayList<String>()
        List<String> specList = new ArrayList<String>();
        //7.遍历读取分组查询结果-stringTerms.getBuckets().for
        for (StringTerms.Bucket bucket : stringTerms.getBuckets()) {
            //7.1获取分类名字，并将分类名字存入到集合中-bucket.getKeyAsString()
            specList.add(bucket.getKeyAsString());
        }

        Map<String, Set<String>> specMap = new HashMap<String, Set<String>>();
        //把spec转Map
        for (String json : specList) {
            //{"电视音响效果":"小影院","电视屏幕尺寸":"20英寸","尺码":"165"}
            Map<String,String> tempMap = JSON.parseObject(json, Map.class);
            for (String key : tempMap.keySet()) {
                Set<String> tempSet = specMap.get(key);
                //第一次运算，Set是为空的
                if(tempSet == null || tempSet.size() < 1){
                    tempSet = new HashSet<String>();
                }
                //追加元索
                tempSet.add(tempMap.get(key));
                //记录元索
                specMap.put(key, tempSet);
            }
        }
        //把规格返回
        map.put("specMap", specMap);
    }

    /**
     * 聚合查询-分组
     * 根据查询条件，分组查询品牌列表
     * @param map 结果集
     * @param builder 查询条件构建器
     */
    private void searchBrandList(Map<String, Object> map, NativeSearchQueryBuilder builder) {
        //1.设置分组域名-termsAggregationBuilder = AggregationBuilders.terms(别名).field(域名);
        TermsAggregationBuilder termsAggregationBuilder = AggregationBuilders.terms("group_brand").field("brandName");
        //2.添加分组查询参数-builder.addAggregation(termsAggregationBuilder)
        builder.addAggregation(termsAggregationBuilder);
        //3.执行搜索-esTemplate.queryForPage(builder.build(), SkuInfo.class)
        AggregatedPage<SkuInfo> page = esTemplate.queryForPage(builder.build(), SkuInfo.class);
        //4.获取所有分组查询结果集-page.getAggregations()
        Aggregations aggregations = page.getAggregations();
        //5.提取分组结果数据-stringTerms = aggregations.get(填入刚才查询时的别名)
        StringTerms stringTerms = aggregations.get("group_brand");
        //6.定义分类名字列表-categoryList = new ArrayList<String>()
        List<String> brandList = new ArrayList<String>();
        //7.遍历读取分组查询结果-stringTerms.getBuckets().for
        for (StringTerms.Bucket bucket : stringTerms.getBuckets()) {
            //7.1获取分类名字，并将分类名字存入到集合中-bucket.getKeyAsString()
            brandList.add(bucket.getKeyAsString());
        }
        //8.返回分类数据列表-map.put("categoryList", categoryList)
        map.put("brandList", brandList);
    }

    /**
     * 聚合查询-分组
     * 根据查询条件，分组查询分类列表
     * @param map 结果集
     * @param builder 查询条件构建器
     */
    private void searchCategoryList(Map<String, Object> map, NativeSearchQueryBuilder builder) {
        //1.设置分组域名-termsAggregationBuilder = AggregationBuilders.terms(别名).field(域名);
        TermsAggregationBuilder termsAggregationBuilder = AggregationBuilders.terms("group_category").field("categoryName");
        //2.添加分组查询参数-builder.addAggregation(termsAggregationBuilder)
        builder.addAggregation(termsAggregationBuilder);
        //3.执行搜索-esTemplate.queryForPage(builder.build(), SkuInfo.class)
        AggregatedPage<SkuInfo> page = esTemplate.queryForPage(builder.build(), SkuInfo.class);
        //4.获取所有分组查询结果集-page.getAggregations()
        Aggregations aggregations = page.getAggregations();
        //5.提取分组结果数据-stringTerms = aggregations.get(填入刚才查询时的别名)
        StringTerms stringTerms = aggregations.get("group_category");
        //6.定义分类名字列表-categoryList = new ArrayList<String>()
        List<String> categoryList = new ArrayList<String>();
        //7.遍历读取分组查询结果-stringTerms.getBuckets().for
        for (StringTerms.Bucket bucket : stringTerms.getBuckets()) {
            //7.1获取分类名字，并将分类名字存入到集合中-bucket.getKeyAsString()
            categoryList.add(bucket.getKeyAsString());
        }
        //8.返回分类数据列表-map.put("categoryList", categoryList)
        map.put("categoryList", categoryList);
    }

    /**
     * 根据查询条件，查询商品列表
     * @param map 结果集
     * @param builder 查询条件构建器
     */
    private void searchList(Map<String, Object> map, NativeSearchQueryBuilder builder) {


        //没有高亮前的查询方式
        //4.查询数据-esTemplate.queryForPage(条件对象,搜索结果对象)
        //AggregatedPage<SkuInfo> page = esTemplate.queryForPage(query, SkuInfo.class);

        //高亮查询方式：开始..............
        //h1.配置高亮查询信息-hField = new HighlightBuilder.Field()
        //h1.1:设置高亮域名-在构造函数中设置
        HighlightBuilder.Field hField = new HighlightBuilder.Field("name");
        //h1.2：设置高亮前缀-hField.preTags
        hField.preTags("<em style='color:red;'>");
        //h1.3：设置高亮后缀-hField.postTags
        hField.postTags("</em>");
        //h1.4：设置碎片大小-hField.fragmentSize
        hField.fragmentSize(100);  //字符的个数
        //h1.5：追加高亮查询信息-builder.withHighlightFields()
        builder.withHighlightFields(hField);

        ///--------注意，此处代码必须放在高亮条件设置之后--------
        //3、获取NativeSearchQuery搜索条件对象-builder.build()
        NativeSearchQuery query = builder.build();

        //h2.高亮数据读取-AggregatedPage<SkuInfo> page = esTemplate.queryForPage(query, SkuInfo.class, new SearchResultMapper(){})
        AggregatedPage<SkuInfo> page = esTemplate.queryForPage(query, SkuInfo.class, new SearchResultMapper() {
            //h2.1实现mapResults(查询到的结果,数据列表的类型,分页选项)方法
            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
                //h2.2 先定义一组查询结果列表-List<T> list = new ArrayList<T>()
                //T:是当前的数据类型，在此就是我们的SkuInfo
                List<T> list = new ArrayList<T>();
                //h2.3 遍历查询到的所有高亮数据-response.getHits().for
                for (SearchHit hit : response.getHits()) {
                    //h2.3.1 先获取当次结果的原始数据(无高亮)-hit.getSourceAsString()
                    String json = hit.getSourceAsString();
                    //h2.3.2 把json串转换为SkuInfo对象-skuInfo = JSON.parseObject()
                    SkuInfo skuInfo = JSON.parseObject(json, SkuInfo.class);
                    //h2.3.3 获取name域的高亮数据-nameHighlight = hit.getHighlightFields().get("name")
                    HighlightField nameHighlight = hit.getHighlightFields().get("name");
                    //h2.3.4 如果高亮数据不为空-读取高亮数据
                    if(nameHighlight != null) {
                        //h2.3.4.1 定义一个StringBuffer用于存储高亮碎片-buffer = new StringBuffer()
                        StringBuffer buffer = new StringBuffer();
                        //h2.3.4.2 循环组装高亮碎片数据- nameHighlight.getFragments().for(追加数据)
                        for (Text fragment : nameHighlight.getFragments()) {
                            buffer.append(fragment);
                        }
                        //h2.3.4.3 将非高亮数据替换成高亮数据-skuInfo.setName()
                        skuInfo.setName(buffer.toString());
                    }
                    //h2.3.5 将替换了高亮数据的对象封装到List中-list.add((T) esItem)
                    list.add((T) skuInfo);
                }
                //h2.4 返回当前方法所需要参数-new AggregatedPageImpl<T>(数据列表，分页选项,总记录数)
                //h2.4 参考new AggregatedPageImpl<T>(list,pageable,response.getHits().getTotalHits())
                return new AggregatedPageImpl<T>(list,pageable,response.getHits().getTotalHits());
            }
        });
        //5、包装结果并返回
        map.put("rows", page.getContent());  //商品列表
        map.put("total", page.getTotalElements());  //总记录数
        map.put("totalPages", page.getTotalPages());  //总页数

        //Pageable页码从0开始
        int pageNum = query.getPageable().getPageNumber() + 1;  //当前页
        map.put("pageNum", pageNum);
        int pageSize = query.getPageable().getPageSize();//每页查询的条数
        map.put("pageSize", pageSize);
    }

    /**
     * 构建基本查询条件
     * @param searchMap 用户传入的查询条件Map对象
     *                  keywords:关键字
     *                  category:分类
     *                  brand:品牌
     *                  spec_网络制式:移动4G
     *                  spec_.....等等规格参数
     *                  price:0-500 | 500-100 | 3000
     * @return 查询条件构建器
     */
    private NativeSearchQueryBuilder builderBasicQuery(Map<String, String> searchMap) {
        //1、创建查询条件构建器-builder = new NativeSearchQueryBuilder()
        NativeSearchQueryBuilder builder = new NativeSearchQueryBuilder();
        //2、组装查询条件
        if(searchMap != null){

            //使用Bool组装多个查询条件
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            //2.1关键字搜索-builder.withQuery(QueryBuilders.matchQuery(域名，内容))
            if (StringUtils.isNotBlank(searchMap.get("keywords"))) {
                //分词匹配查询
                //builder.withQuery(QueryBuilders.matchQuery("name", searchMap.get("keywords")));
                //追加一个must条件
                boolQueryBuilder.must(QueryBuilders.matchQuery("name", searchMap.get("keywords")));
            }
            //2.2 分类搜索
            if (StringUtils.isNotBlank(searchMap.get("category"))) {
                boolQueryBuilder.must(QueryBuilders.termQuery("categoryName", searchMap.get("category")));
            }
            //2.3 品牌搜索
            if (StringUtils.isNotBlank(searchMap.get("brand"))) {
                boolQueryBuilder.must(QueryBuilders.termQuery("brandName", searchMap.get("brand")));
            }
            //2.4 规格搜索
            for (String key : searchMap.keySet()) {
                //找到key以spec_打头
                //spec_网络制式
                if (key.startsWith("spec_")) {
                    String specField = "specMap." + key.substring(key.indexOf("_") + 1) + ".keyword";
                    //System.out.println(specField);
                    boolQueryBuilder.must(QueryBuilders.termQuery(specField, searchMap.get(key)));
                }
            }
            //2.5 价格区间搜索
            if (StringUtils.isNotBlank(searchMap.get("price"))) {
                //price:0-500 | 500-100 | 3000
                String[] prices = searchMap.get("price").split("-");
                //范围匹配构建器
                RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("price");
                // 0 <= price <= 500
                rangeQueryBuilder.gte(prices[0]);
                if(prices.length > 1){
                    rangeQueryBuilder.lte(prices[1]);
                }
                //追加范围条件
                boolQueryBuilder.must(rangeQueryBuilder);
            }
            //设置多条件查询
            builder.withQuery(boolQueryBuilder);

            //2.6 分页查询---没有排序功能
            //当前页，默认第一页
            Integer pageNum = StringUtils.isBlank(searchMap.get("pageNum")) ? 1 : new Integer(searchMap.get("pageNum"));
            //每页查询的条数
            Integer pageSize = 5;
            //of(当前页[从0算起]，每页查询的记录数)
            Pageable pageable = PageRequest.of(pageNum - 1,pageSize);
            builder.withPageable(pageable);

            //2.6 分页查询--有排序功能-排序方式一
            /*//当前页，默认第一页
            Integer pageNum = StringUtils.isBlank(searchMap.get("pageNum")) ? 1 : new Integer(searchMap.get("pageNum"));
            //每页查询的条数
            Integer pageSize = 5;
            //设置排序条件
            Sort sort = null;
            if(StringUtils.isNotBlank(searchMap.get("sortField"))){
                //升序
                if("ASC".equalsIgnoreCase(searchMap.get("sortRule"))){
                    sort = new Sort(Sort.Direction.ASC,searchMap.get("sortField"));
                }else{ //降序
                    sort = new Sort(Sort.Direction.DESC,searchMap.get("sortField"));
                }
            }
            //of(当前页[从0算起]，每页查询的记录数)
            Pageable pageable = PageRequest.of(pageNum - 1,pageSize,sort);
            builder.withPageable(pageable);*/

            //2.7 排序实现,方式二(推荐)
            if(StringUtils.isNotBlank(searchMap.get("sortField")) &&
                    StringUtils.isNotBlank(searchMap.get("sortRule"))) {
                /**
                 * 设置排序条件
                 * SortBuilders.fieldSort(域名).order(SortOrder.valueOf(排序方式★))
                 * ★:此处的参数必须是大写的，所以我们把用户传入的参数.toUpperCase()
                 * 除此外，.order还有一种用法.order(SortOrder.ASC)，不灵活，不推荐
                 */
                SortBuilder sortBuilder = SortBuilders.fieldSort(searchMap.get("sortField"))
                        .order(SortOrder.valueOf(searchMap.get("sortRule").toUpperCase()));
                builder.withSort(sortBuilder);
            }
        }
        return builder;
    }
}
