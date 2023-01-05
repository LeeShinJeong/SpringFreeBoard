package com.sparta.hanghaememo.service;

import com.sparta.hanghaememo.dto.MemoRequestDto;
import com.sparta.hanghaememo.dto.MemoResponseDto;
import com.sparta.hanghaememo.dto.StatusResponse;
import com.sparta.hanghaememo.entity.Memo;
import com.sparta.hanghaememo.entity.User;
import com.sparta.hanghaememo.jwt.JwtUtil;
import com.sparta.hanghaememo.repository.MemoRepository;
import com.sparta.hanghaememo.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static com.sparta.hanghaememo.entity.UserRoleEnum.ADMIN;

@Service
@RequiredArgsConstructor
public class MemoService {
    private final MemoRepository memoRepository;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Transactional
    public MemoResponseDto createMemo(MemoRequestDto memorequestDto, HttpServletRequest request) {
        String token = jwtUtil.resolveToken(request);
        Claims claims;

        if (token != null) {
            if (jwtUtil.validateToken(token)) {
                claims = jwtUtil.getUserInfoFromToken(token);
            } else {
                throw new IllegalArgumentException("Token Error");
            }
        }
            Memo memo = new Memo(memorequestDto);
            memoRepository.save(memo);
            MemoResponseDto memoResponseDto = new MemoResponseDto(memo.getCreatedAt(), memo.getModifiedAt(), memo.getUsers(), memo.getTitles(), memo.getContents());
            return memoResponseDto;

    }

    @Transactional(readOnly = true)
    public List<Memo> getMemo() {
        return memoRepository.findAllByOrderByModifiedAtDesc();
    }

    @Transactional(readOnly = true)
    public MemoResponseDto getDetail(Long id){
        Memo memo = checkMemo(id);
        return new MemoResponseDto(memo.getCreatedAt(),memo.getModifiedAt(),memo.getUsers(),memo.getTitles(),memo.getContents());
    }


    @Transactional
    public MemoResponseDto update(Long id, MemoRequestDto requestDto) {
        Memo memo = memoRepository.findByIdAndPasswords(id,requestDto.getPasswords());
        if(!memo.equals(null)) {
            memo.update(requestDto);
        }
        else if(memo.equals(null)){
            return null;
        }
        MemoResponseDto memoResponseDto = new MemoResponseDto(memo.getCreatedAt(),memo.getModifiedAt(),memo.getUsers(),memo.getTitles(),memo.getContents());
        return memoResponseDto;
    }

    @Transactional
    public StatusResponse deleteMemo(Long id, HttpServletRequest request) {
        Memo memo = checkMemo(id);
        String token = jwtUtil.resolveToken(request);
            Claims claims;
            if (token != null) {
                if (jwtUtil.validateToken(token)) {claims = jwtUtil.getUserInfoFromToken(token);} else {throw new IllegalArgumentException("Token Error");}

                User user = userRepository.findByUsername(claims.getSubject()).orElseThrow(() -> new IllegalArgumentException("등록된 사용자가 없습니다."));

                if (user.getRole().equals(ADMIN)) {memoRepository.deleteById(id);
                }else{
                    if (claims.getSubject().equals(memo.getUsers())) {memoRepository.deleteById(id);
                    }else{throw new IllegalArgumentException("접근 권한이 없습니다.");}

                }return new StatusResponse("게시글 삭제 완료", HttpStatus.OK.value()); }else{return null;}
        }


    private Memo checkMemo(Long id){
        return memoRepository.findById(id).orElseThrow(
                ()->new IllegalArgumentException("게시글일 존재하지 않습니다.")
        );
    }

    private static boolean checkPasswords(MemoRequestDto memoRequestDto,Memo memo){
        return memo.getPasswords().equals(memoRequestDto.getPasswords());
    }

}
