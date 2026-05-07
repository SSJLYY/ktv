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
        assertSingerNameUnique(singerDTO.getName(), null);

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

        int inserted = singerMapper.insert(singer);
        if (inserted <= 0) {
            throw new BusinessException("歌手创建失败");
        }
        return singer.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateSinger(Long id, SingerDTO singerDTO) {
        Singer existSinger = loadSinger(id);
        String targetName = singerDTO.getName() != null ? singerDTO.getName() : existSinger.getName();
        assertSingerNameUnique(targetName, id);

        Singer singer = new Singer();
        BeanUtils.copyProperties(singerDTO, singer);
        singer.setId(id);
        if (singer.getName() == null) {
            singer.setName(existSinger.getName());
        }
        if (singer.getGender() == null) {
            singer.setGender(existSinger.getGender());
        }
        if (singer.getRegion() == null) {
            singer.setRegion(existSinger.getRegion());
        }
        if (singer.getAvatar() == null) {
            singer.setAvatar(existSinger.getAvatar());
        }
        if (singer.getStatus() == null) {
            singer.setStatus(existSinger.getStatus());
        }
        singer.setSongCount(existSinger.getSongCount());

        if (!existSinger.getName().equals(singer.getName())) {
            singer.setPinyin(PinyinUtil.getPinyin(singer.getName()));
            singer.setPinyinInitial(PinyinUtil.getPinyinInitial(singer.getName()));
        } else {
            singer.setPinyin(existSinger.getPinyin());
            singer.setPinyinInitial(existSinger.getPinyinInitial());
        }

        boolean updated = singerMapper.updateById(singer) > 0;
        if (!updated) {
            throw new BusinessException("歌手更新失败");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteSinger(Long id) {
        Singer singer = loadSinger(id);
        if (singer.getSongCount() != null && singer.getSongCount() > 0) {
            throw new BusinessException("该歌手下还有歌曲，无法删除");
        }

        boolean deleted = singerMapper.deleteById(id) > 0;
        if (!deleted) {
            throw new BusinessException("歌手删除失败");
        }
        return true;
    }

    @Override
    public SingerVO getSingerById(Long id) {
        Singer singer = loadSinger(id);
        SingerVO singerVO = new SingerVO();
        BeanUtils.copyProperties(singer, singerVO);
        return singerVO;
    }

    private Singer loadSinger(Long id) {
        Singer singer = singerMapper.selectById(id);
        if (singer == null) {
            throw new BusinessException("歌手不存在");
        }
        return singer;
    }

    private void assertSingerNameUnique(String name, Long excludeId) {
        LambdaQueryWrapper<Singer> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Singer::getName, name);
        if (excludeId != null) {
            queryWrapper.ne(Singer::getId, excludeId);
        }
        Long count = singerMapper.selectCount(queryWrapper);
        if (count != null && count > 0) {
            throw new BusinessException("歌手名称已存在");
        }
    }
}
