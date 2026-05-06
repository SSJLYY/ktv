package com.ktv.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ktv.common.exception.BusinessException;
import com.ktv.common.util.PinyinUtil;
import com.ktv.dto.SingerDTO;
import com.ktv.entity.Singer;
import com.ktv.mapper.SingerMapper;
import com.ktv.service.SingerService;
import com.ktv.vo.SingerVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 歌手服务实现。
 */
@Service
@RequiredArgsConstructor
public class SingerServiceImpl extends ServiceImpl<SingerMapper, Singer> implements SingerService {

    private final SingerMapper singerMapper;

    @Override
    public IPage<SingerVO> getSingerPage(Integer current, Integer size, String name, String region) {
        Page<SingerVO> page = new Page<>(current, size);
        return singerMapper.selectPageWithConditions(page, name, region);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSinger(SingerDTO singerDTO) {
        LambdaQueryWrapper<Singer> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Singer::getName, singerDTO.getName());
        Long count = singerMapper.selectCount(queryWrapper);
        if (count != null && count > 0) {
            throw new BusinessException("歌手名已存在");
        }

        Singer singer = new Singer();
        BeanUtils.copyProperties(singerDTO, singer);
        singer.setPinyin(PinyinUtil.getPinyin(singer.getName()));
        singer.setPinyinInitial(PinyinUtil.getPinyinInitial(singer.getName()));

        if (singer.getGender() == null) {
            singer.setGender(0);
        }
        if (singer.getStatus() == null) {
            singer.setStatus(1);
        }
        singer.setSongCount(0);

        singerMapper.insert(singer);
        return singer.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateSinger(Long id, SingerDTO singerDTO) {
        Singer existSinger = singerMapper.selectById(id);
        if (existSinger == null) {
            throw new BusinessException("歌手不存在");
        }

        if (singerDTO.getName() == null) {
            singerDTO.setName(existSinger.getName());
        }
        if (!existSinger.getName().equals(singerDTO.getName())) {
            LambdaQueryWrapper<Singer> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Singer::getName, singerDTO.getName());
            queryWrapper.ne(Singer::getId, id);
            Long count = singerMapper.selectCount(queryWrapper);
            if (count != null && count > 0) {
                throw new BusinessException("歌手名已存在");
            }
        }

        Singer singer = new Singer();
        BeanUtils.copyProperties(singerDTO, singer);
        singer.setId(id);

        if (!existSinger.getName().equals(singerDTO.getName())) {
            singer.setPinyin(PinyinUtil.getPinyin(singerDTO.getName()));
            singer.setPinyinInitial(PinyinUtil.getPinyinInitial(singerDTO.getName()));
        } else {
            singer.setPinyin(existSinger.getPinyin());
            singer.setPinyinInitial(existSinger.getPinyinInitial());
        }

        return singerMapper.updateById(singer) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteSinger(Long id) {
        Singer singer = singerMapper.selectById(id);
        if (singer == null) {
            throw new BusinessException("歌手不存在");
        }
        if (singer.getSongCount() != null && singer.getSongCount() > 0) {
            throw new BusinessException("该歌手下还有歌曲，无法删除");
        }
        return singerMapper.deleteById(id) > 0;
    }

    @Override
    public SingerVO getSingerById(Long id) {
        Singer singer = singerMapper.selectById(id);
        if (singer == null) {
            throw new BusinessException("歌手不存在");
        }

        SingerVO singerVO = new SingerVO();
        BeanUtils.copyProperties(singer, singerVO);
        return singerVO;
    }
}
