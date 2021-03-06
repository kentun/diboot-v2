package diboot.core.test.binder.mapper;

import com.diboot.core.mapper.BaseCrudMapper;
import diboot.core.test.binder.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 员工Mapper
 * @author mazc@dibo.ltd
 * @version 2018/12/22
 */
@Mapper
public interface UserMapper extends BaseCrudMapper<User> {

}

