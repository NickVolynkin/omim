#pragma once

#include "../base/base.hpp"
#include "../std/algorithm.hpp"
#include "../std/functional.hpp"
#include "../std/vector.hpp"

namespace my
{

// Priority queue that stores only N smallest elements.
template <typename T, typename CompareT = less<T> >
class limited_priority_queue
{
public:
  typedef T value_type;
  typedef typename vector<T>::const_iterator const_iterator;

  explicit limited_priority_queue(size_t maxSize = 1, CompareT compare = CompareT())
    : m_maxSize(maxSize == 0 ? 1 : maxSize), m_compare(compare)
  {
  }

  void push(T const & t)
  {
    if (m_queue.size() < m_maxSize)
    {
      m_queue.push_back(t);
      push_heap(m_queue.begin(), m_queue.end(), m_compare);
    }
    else if (m_compare(t, m_queue.back()))
    {
      // This can be optimized by writing decrease_head_heap().
      pop_heap(m_queue.begin(), m_queue.end(), m_compare);
      m_queue.back() = t;
      push_heap(m_queue.begin(), m_queue.end(), m_compare);
    }
  }

  void pop()
  {
    pop_heap(m_queue.begin(), m_queue.end(), m_compare);
    m_queue.pop_back();
  }

  void set_max_size(size_t maxSize)
  {
    // This can be optimized by writing pop_n_heap().
    m_maxSize = (maxSize == 0 ? 1 : maxSize);
    while (size() > m_maxSize)
      pop();
  }

  size_t max_size() const { return m_maxSize; }
  bool empty() const { return m_queue.empty(); }
  size_t size() const { return m_queue.size(); }
  T const & top() const { return m_queue.back(); }

  const_iterator begin() const { return m_queue.begin(); }
  const_iterator end() const { return m_queue.end(); }

  void clear() { m_queue.clear(); }

  void swap(limited_priority_queue<T, CompareT> & queue)
  {
    m_queue.swap(queue.m_queue);
    using std::swap;
    swap(m_maxSize, queue.m_maxSize);
    swap(m_compare, queue.m_compare);
  }

private:
  vector<T> m_queue;
  size_t m_maxSize;
  CompareT m_compare;
};

template <typename T, typename CompareT>
void swap(limited_priority_queue<T, CompareT> & q1, limited_priority_queue<T, CompareT> & q2)
{
  q1.swap(q2);
}

}  // namespace my
