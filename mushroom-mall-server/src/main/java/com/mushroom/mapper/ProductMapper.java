package com.mushroom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mushroom.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 原子扣减库存并累加销量
     *
     * 原实现是"先查出来、再 setStock(旧值 - 数量) 写回"，两个并发请求会读到同一个旧值，
     * 各自判断"库存够"，最终卖超。这里改成数据库单条 UPDATE：
     *   - stock >= #{number} 作为更新条件，扣减与判断在同一语句内完成，不会出现负数
     *   - sale_count 同样用 sale_count + #{number} 自增，避免并发丢失更新
     * 返回受影响行数：0 表示库存不足（或被逻辑删除），调用方据此抛业务异常触发回滚。
     *
     * 注意：@TableLogic 对自定义 SQL 不生效，需显式带上 is_deleted = 0。
     */
    @Update("UPDATE product SET stock = stock - #{number}, sale_count = sale_count + #{number} "
            + "WHERE id = #{id} AND stock >= #{number} AND is_deleted = 0")
    int deductStockAndAddSale(@Param("id") Long id, @Param("number") Integer number);

    /**
     * 原子恢复库存并回退销量（取消订单 / 同意退款用）
     *
     * 与扣减保持对称：原实现是「查出来 → stock + number → 写回」，
     * 并发下两个取消请求读到同一个旧值，会丢掉一次恢复。
     * 这里同样放进单条 UPDATE：
     *   - stock 直接自增，不需要读旧值
     *   - sale_count 用 GREATEST(...,0) 兜住历史脏数据，避免负销量
     *   - 显式带 is_deleted = 0，因为 @TableLogic 对自定义 SQL 不生效
     */
    @Update("UPDATE product SET stock = stock + #{number}, "
            + "sale_count = GREATEST(sale_count - #{number}, 0) "
            + "WHERE id = #{id} AND is_deleted = 0")
    int restoreStockAndReduceSale(@Param("id") Long id, @Param("number") Integer number);
}
