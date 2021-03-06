package com.diboot.iam.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.diboot.core.binding.RelationsBinder;
import com.diboot.core.exception.BusinessException;
import com.diboot.core.util.BeanUtils;
import com.diboot.core.util.S;
import com.diboot.core.util.V;
import com.diboot.core.vo.Status;
import com.diboot.iam.config.Cons;
import com.diboot.iam.dto.IamUserAccountDTO;
import com.diboot.iam.entity.*;
import com.diboot.iam.mapper.IamUserMapper;
import com.diboot.iam.service.IamAccountService;
import com.diboot.iam.service.IamFrontendPermissionService;
import com.diboot.iam.service.IamUserRoleService;
import com.diboot.iam.service.IamUserService;
import com.diboot.iam.util.IamSecurityUtils;
import com.diboot.iam.vo.IamRoleVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
* 系统用户相关Service实现
* @author mazc@dibo.ltd
* @version 2.0
* @date 2019-12-17
*/
@Service
@Slf4j
public class IamUserServiceImpl extends BaseIamServiceImpl<IamUserMapper, IamUser> implements IamUserService {

    @Autowired
    private IamUserRoleService iamUserRoleService;

    @Autowired
    private IamFrontendPermissionService iamFrontendPermissionService;

    @Autowired
    private IamAccountService iamAccountService;

    @Override
    public IamRoleVO buildRoleVo4FrontEnd(IamUser iamUser) {
        List<IamRoleVO> roleVOList = getAllRoleVOList(iamUser);
        if (V.isEmpty(roleVOList)){
            return null;
        }
        // 附加额外的一些权限给与特性的角色
        attachExtraPermissions(roleVOList);
        // 对RoleList做聚合处理，以适配前端
        List<String> nameList = new ArrayList<>();
        List<String> codeList = new ArrayList<>();
        List<IamFrontendPermission> allPermissionList = new ArrayList<>();
        roleVOList.forEach(vo -> {
            nameList.add(vo.getName());
            codeList.add(vo.getCode());
            if (V.notEmpty(vo.getPermissionList())){
                allPermissionList.addAll(vo.getPermissionList());
            }
        });
        // 对permissionList进行去重
        List permissionList = BeanUtils.distinctByKey(allPermissionList, IamFrontendPermission::getId);
        IamRoleVO roleVO = new IamRoleVO();
        roleVO.setName(S.join(nameList));
        roleVO.setCode(S.join(codeList));
        roleVO.setPermissionList(permissionList);

        return roleVO;
    }

    @Override
    public List<IamRoleVO> getAllRoleVOList(IamUser iamUser) {
        List<IamRole> roleList = iamUserRoleService.getUserRoleList(IamUser.class.getSimpleName(), iamUser.getId());
        if (V.isEmpty(roleList)){
            return null;
        }
        return RelationsBinder.convertAndBind(roleList, IamRoleVO.class);
    }

    @Override
    public void attachExtraPermissions(List<IamRoleVO> roleVOList) {
        if (V.isEmpty(roleVOList)){
            return;
        }
        for (IamRoleVO roleVO : roleVOList){
            if (Cons.ROLE_SUPER_ADMIN.equalsIgnoreCase(roleVO.getCode())){
                List<IamFrontendPermission> iamPermissions = iamFrontendPermissionService.getAllFrontendPermissions(Cons.APPLICATION);
                roleVO.setPermissionList(iamPermissions);
                return;
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createUserAndAccount(IamUserAccountDTO userAccountDTO) throws Exception {
        // 创建用户信息
        this.createEntity(userAccountDTO);
        // 如果提交的有账号信息，则新建账号信息
        if (V.notEmpty(userAccountDTO.getUsername())) {
            // 新建account账号
            this.createAccount(userAccountDTO);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUserAndAccount(IamUserAccountDTO userAccountDTO) throws Exception {
        // 更新用户信息
        this.updateEntity(userAccountDTO);

        IamAccount iamAccount = iamAccountService.getSingleEntity(
                Wrappers.<IamAccount>lambdaQuery()
                        .eq(IamAccount::getUserType, IamUser.class.getSimpleName())
                        .eq(IamAccount::getUserId, userAccountDTO.getId())
        );

        if (iamAccount == null) {
            if (V.isEmpty(userAccountDTO.getUsername())){
                return true;
            } else {
                // 新建account账号
                this.createAccount(userAccountDTO);
            }
        } else {
            if (V.isEmpty(userAccountDTO.getUsername())) {
                // 删除账号
                this.deleteAccount(userAccountDTO.getId());
            } else {
                // 更新账号
                iamAccount.setAuthAccount(userAccountDTO.getUsername())
                        .setStatus(userAccountDTO.getStatus());
                // 设置密码
                if (V.notEmpty(userAccountDTO.getPassword())){
                    iamAccount.setAuthSecret(userAccountDTO.getPassword());
                    IamSecurityUtils.encryptPwd(iamAccount);
                }
                iamAccountService.updateEntity(iamAccount);

                // 批量更新角色关联关系
                iamUserRoleService.updateUserRoleRelations(iamAccount.getUserType(), iamAccount.getUserId(), userAccountDTO.getRoleIdList());
            }
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUserAndAccount(Long id) throws Exception {
        IamUser iamUser = this.getEntity(id);
        if (iamUser == null){
            throw new BusinessException(Status.FAIL_OPERATION, "删除的记录不存在");
        }
        // 删除用户信息
        this.deleteEntity(id);
        // 删除账号信息
        this.deleteAccount(id);

        return true;
    }

    private void createAccount(IamUserAccountDTO userAccountDTO) throws Exception{
        // 创建账号信息
        IamAccount iamAccount = new IamAccount();
        iamAccount
                .setUserType(IamUser.class.getSimpleName())
                .setUserId(userAccountDTO.getId())
                .setAuthAccount(userAccountDTO.getUsername())
                .setAuthSecret(userAccountDTO.getPassword())
                .setAuthType(Cons.DICTCODE_AUTH_TYPE.PWD.name())
                .setStatus(userAccountDTO.getStatus());
        // 保存账号
        iamAccountService.createEntity(iamAccount);

        // 批量创建角色关联关系
        iamUserRoleService.createUserRoleRelations(iamAccount.getUserType(), iamAccount.getUserId(), userAccountDTO.getRoleIdList());
    }

    private void deleteAccount(Long userId) throws Exception {
        // 删除账号信息
        iamAccountService.deleteEntities(
                Wrappers.<IamAccount>lambdaQuery()
                        .eq(IamAccount::getUserType, IamUser.class.getSimpleName())
                        .eq(IamAccount::getUserId, userId)
        );
        // 删除用户角色关联关系列表
        iamUserRoleService.deleteEntities(
                Wrappers.<IamUserRole>lambdaQuery()
                        .eq(IamUserRole::getUserType, IamUser.class.getSimpleName())
                        .eq(IamUserRole::getUserId, userId)
        );
    }

}
