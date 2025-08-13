import React, { useState, useEffect } from 'react';
import { 
  View, 
  Text, 
  StyleSheet, 
  FlatList, 
  TouchableOpacity, 
  RefreshControl,
  Alert,
  ScrollView
} from 'react-native';
import { router, useFocusEffect } from 'expo-router';
import AsyncStorage from '@react-native-async-storage/async-storage';
import axios from 'axios';
import { API_BASE_URL } from '../utils/apiConfig';
import { Ionicons } from '@expo/vector-icons';
import { useLanguage } from '../utils/LanguageContext';
import { useTranslation } from 'react-i18next';

const TaskManagement = () => {
  const { language } = useLanguage();
  const { t, i18n } = useTranslation();

  // Update i18n language when language changes
  useEffect(() => {
    i18n.changeLanguage(language);
  }, [language, i18n]);

  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [filter, setFilter] = useState('all'); // all, pending, completed, overdue
  const [stats, setStats] = useState({
    totalTasks: 0,
    completedTasks: 0,
    pendingTasks: 0,
    overdueTasks: 0
  });

  // Fetch tasks from API
  const fetchTasks = async () => {
    try {
      const token = await AsyncStorage.getItem('authToken');
      if (!token) {
        router.replace('/');
        return;
      }

      const response = await axios.get(`${API_BASE_URL}/api/tasks`, {
        headers: { Authorization: `Bearer ${token}` }
      });

      setTasks(response.data);
      calculateStats(response.data);
    } catch (error) {
      console.error('Error fetching tasks:', error);
      Alert.alert(t('taskManagement.error'), t('taskManagement.failedToFetchTasks'));
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  // Calculate task statistics
  const calculateStats = (taskList) => {
    const now = new Date();
    const completed = taskList.filter(task => task.isCompleted).length;
    const pending = taskList.filter(task => !task.isCompleted).length;
    const overdue = taskList.filter(task => {
      const taskDate = new Date(task.dueDate);
      return taskDate < now && !task.isCompleted;
    }).length;

    setStats({
      totalTasks: taskList.length,
      completedTasks: completed,
      pendingTasks: pending,
      overdueTasks: overdue
    });
  };

  // Filter tasks based on selected filter
  const getFilteredTasks = () => {
    const now = new Date();
    
    switch (filter) {
      case 'pending':
        return tasks.filter(task => !task.isCompleted);
      case 'completed':
        return tasks.filter(task => task.isCompleted);
      case 'overdue':
        return tasks.filter(task => {
          const taskDate = new Date(task.dueDate);
          return taskDate < now && !task.isCompleted;
        });
      default:
        return tasks;
    }
  };

  // Toggle task completion
  const toggleTaskCompletion = async (taskId) => {
    try {
      const token = await AsyncStorage.getItem('authToken');
      await axios.patch(`${API_BASE_URL}/api/tasks/${taskId}/toggle`, {}, {
        headers: { Authorization: `Bearer ${token}` }
      });
      
      fetchTasks(); // Refresh tasks
    } catch (error) {
      console.error('Error toggling task:', error);
      Alert.alert(t('taskManagement.error'), t('taskManagement.failedToUpdateTask'));
    }
  };

  // Delete task with confirmation
  const deleteTask = (taskId) => {
    Alert.alert(
      t('taskManagement.deleteTask'),
      t('taskManagement.deleteTaskMessage'),
      [
        { text: t('taskManagement.cancel'), style: 'cancel' },
        {
          text: t('taskManagement.delete'),
          style: 'destructive',
          onPress: async () => {
            try {
              const token = await AsyncStorage.getItem('authToken');
              await axios.delete(`${API_BASE_URL}/api/tasks/${taskId}`, {
                headers: { Authorization: `Bearer ${token}` }
              });
              fetchTasks(); // Refresh tasks
            } catch (error) {
              console.error('Error deleting task:', error);
              Alert.alert(t('taskManagement.error'), t('taskManagement.failedToDeleteTask'));
            }
          }
        }
      ]
    );
  };

  // Format date for display
  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', { 
      month: 'short', 
      day: 'numeric', 
      year: 'numeric' 
    });
  };

  // Get priority color
  const getPriorityColor = (priority) => {
    switch (priority) {
      case 'high': return '#760d0dff';
      case 'medium': return '#bb8519ff';
      case 'low': return '#157615ff';
      default: return '#666';
    }
  };

  // Get category icon
  const getCategoryIcon = (category) => {
    switch (category) {
      case 'feeding': return 'restaurant';
      case 'vaccination': return 'medical';
      case 'health-check': return 'heart';
      case 'breeding': return 'heart-circle';
      case 'maintenance': return 'construct';
      default: return 'clipboard';
    }
  };

  // Check if task is overdue
  const isOverdue = (dueDate, isCompleted) => {
    if (isCompleted) return false;
    const now = new Date();
    const taskDate = new Date(dueDate);
    return taskDate < now;
  };

  // Refresh tasks when screen focuses
  useFocusEffect(
    React.useCallback(() => {
      fetchTasks();
    }, [])
  );

  // Render task item
  const renderTask = ({ item }) => (
    <View style={[
      styles.taskCard,
      item.isCompleted && styles.completedTask,
      isOverdue(item.dueDate, item.isCompleted) && styles.overdueTask
    ]}>
      <View style={styles.taskHeader}>
        <View style={styles.taskHeaderLeft}>
          <TouchableOpacity
            onPress={() => toggleTaskCompletion(item._id)}
            style={[
              styles.checkbox,
              item.isCompleted && styles.checkedCheckbox
            ]}
          >
            {item.isCompleted && (
              <Ionicons name="checkmark" size={16} color="#fff" />
            )}
          </TouchableOpacity>
          
          <View style={styles.taskInfo}>
            <Text style={[
              styles.taskTitle,
              item.isCompleted && styles.completedText
            ]}>
              {item.title}
            </Text>
            <Text style={styles.taskDate}>
              {formatDate(item.dueDate)} at {item.dueTime}
            </Text>
          </View>
        </View>
        
        <View style={styles.taskHeaderRight}>
          <View style={[
            styles.priorityBadge,
            { backgroundColor: getPriorityColor(item.priority) }
          ]}>
            <Text style={styles.priorityText}>
              {item.priority.toUpperCase()}
            </Text>
          </View>
        </View>
      </View>

      <View style={styles.taskDetails}>
        <View style={styles.taskMeta}>
          <Ionicons 
            name={getCategoryIcon(item.category)} 
            size={14} 
            color="#666" 
          />
          <Text style={styles.categoryText}>{item.category}</Text>
          
          {item.estimatedCost > 0 && (
            <>
              <Ionicons name="cash" size={14} color="#666" />
              <Text style={styles.costText}>${item.estimatedCost}</Text>
            </>
          )}
          
          {item.animal && (
            <>
              <Ionicons name="paw" size={14} color="#666" />
              <Text style={styles.animalText}>{item.animal.name}</Text>
            </>
          )}
        </View>
        
        {item.description && (
          <Text style={styles.description}>{item.description}</Text>
        )}
      </View>

      <View style={styles.taskActions}>
        <TouchableOpacity
          style={[styles.actionButton, styles.editButton]}
          onPress={() => router.push(`/editTask?id=${item._id}`)}
        >
          <Ionicons name="pencil" size={16} color="#007AFF" />
                      <Text style={styles.editText}>{t('taskManagement.edit')}</Text>
        </TouchableOpacity>
        
        <TouchableOpacity
          style={[styles.actionButton, styles.deleteButton]}
          onPress={() => deleteTask(item._id)}
        >
          <Ionicons name="trash" size={16} color="#ff4444" />
                      <Text style={styles.deleteText}>{t('taskManagement.delete')}</Text>
        </TouchableOpacity>
      </View>
    </View>
  );

  // Render filter buttons
  const renderFilterButtons = () => (
    <ScrollView 
      horizontal 
      showsHorizontalScrollIndicator={false}
      style={styles.filterContainer}
    >
      {[
        { key: 'all', label: `${t('taskManagement.all')} (${stats.totalTasks})` },
        { key: 'pending', label: `${t('taskManagement.pending')} (${stats.pendingTasks})` },
        { key: 'completed', label: `${t('taskManagement.completed')} (${stats.completedTasks})` },
        { key: 'overdue', label: `${t('taskManagement.overdue')} (${stats.overdueTasks})` }
      ].map(filterOption => (
        <TouchableOpacity
          key={filterOption.key}
          style={[
            styles.filterButton,
            filter === filterOption.key && styles.activeFilter
          ]}
          onPress={() => setFilter(filterOption.key)}
        >
          <Text style={[
            styles.filterText,
            filter === filterOption.key && styles.activeFilterText
          ]}>
            {filterOption.label}
          </Text>
        </TouchableOpacity>
      ))}
    </ScrollView>
  );

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
                    <Text>{t('taskManagement.loadingTasks')}</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity
          style={styles.backButton}
          onPress={() => router.back()}
        >
          <Ionicons name="arrow-back" size={24} color="#333" />
        </TouchableOpacity>
        <Text style={styles.title}>{t('taskManagement.title')}</Text>
        <TouchableOpacity
          style={styles.addButton}
          onPress={() => router.push('/addTask')}
        >
          <Ionicons name="add" size={24} color="#fff" />
        </TouchableOpacity>
      </View>

      {renderFilterButtons()}

      <FlatList
        data={getFilteredTasks()}
        renderItem={renderTask}
        keyExtractor={item => item._id}
        contentContainerStyle={styles.listContainer}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={() => {
              setRefreshing(true);
              fetchTasks();
            }}
          />
        }
        ListEmptyComponent={
          <View style={styles.emptyContainer}>
            <Ionicons name="clipboard-outline" size={64} color="#ccc" />
            <Text style={styles.emptyText}>{t('taskManagement.noTasksFound')}</Text>
            <Text style={styles.emptySubText}>
              {t('taskManagement.tapToAddFirstTask')}
            </Text>
          </View>
        }
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 20,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
  },
  backButton: {
    width: 44,
    height: 44,
    borderRadius: 22,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#f0f0f0',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#333',
    flex: 1,
    textAlign: 'center',
  },
  addButton: {
    backgroundColor: '#043c10ff',
    width: 44,
    height: 44,
    borderRadius: 22,
    justifyContent: 'center',
    alignItems: 'center',
  },
  filterContainer: {
    paddingHorizontal: 20,
    paddingVertical: 10,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
  },
  filterButton: {
    paddingHorizontal: 12,
    paddingVertical: 8,
    marginRight: 8,
    borderRadius: 20,
    backgroundColor: '#f0f0f0',
    minWidth: 60,
    alignItems: 'center',
    justifyContent: 'center',
  },
  activeFilter: {
    backgroundColor: '#007AFF',
  },
  filterText: {
    fontSize: 13,
    color: '#666',
    textAlign: 'center',
    fontWeight: '500',
  },
  activeFilterText: {
    color: '#fff',
  },
  listContainer: {
    padding: 20,
  },
  taskCard: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 2,
  },
  completedTask: {
    opacity: 0.7,
  },
  overdueTask: {
    borderLeftWidth: 4,
    borderLeftColor: '#a00505ff',
  },
  taskHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 12,
  },
  taskHeaderLeft: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    flex: 1,
  },
  checkbox: {
    width: 24,
    height: 24,
    borderRadius: 12,
    borderWidth: 2,
    borderColor: '#007AFF',
    marginRight: 12,
    marginTop: 2,
    justifyContent: 'center',
    alignItems: 'center',
  },
  checkedCheckbox: {
    backgroundColor: '#007AFF',
  },
  taskInfo: {
    flex: 1,
  },
  taskTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
    marginBottom: 4,
  },
  completedText: {
    textDecorationLine: 'line-through',
    color: '#666',
  },
  taskDate: {
    fontSize: 14,
    color: '#666',
  },
  taskHeaderRight: {
    alignItems: 'flex-end',
  },
  priorityBadge: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 12,
  },
  priorityText: {
    fontSize: 10,
    fontWeight: '600',
    color: '#fff',
  },
  taskDetails: {
    marginBottom: 12,
  },
  taskMeta: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 8,
  },
  categoryText: {
    fontSize: 12,
    color: '#666',
    marginLeft: 4,
    marginRight: 12,
    textTransform: 'capitalize',
  },
  costText: {
    fontSize: 12,
    color: '#666',
    marginLeft: 4,
    marginRight: 12,
  },
  animalText: {
    fontSize: 12,
    color: '#666',
    marginLeft: 4,
  },
  description: {
    fontSize: 14,
    color: '#666',
    lineHeight: 20,
  },
  taskActions: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    borderTopWidth: 1,
    borderTopColor: '#f0f0f0',
    paddingTop: 12,
  },
  actionButton: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
    borderWidth: 1,
  },
  editButton: {
    borderColor: '#007AFF',
    backgroundColor: '#f0f8ff',
  },
  deleteButton: {
    borderColor: '#a90707ff',
    backgroundColor: '#fff5f5',
  },
  editText: {
    color: '#007AFF',
    marginLeft: 4,
    fontSize: 14,
  },
  deleteText: {
    color: '#ff4444',
    marginLeft: 4,
    fontSize: 14,
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingTop: 100,
  },
  emptyText: {
    fontSize: 18,
    color: '#666',
    marginTop: 16,
  },
  emptySubText: {
    fontSize: 14,
    color: '#999',
    marginTop: 8,
    textAlign: 'center',
  },
});

export default TaskManagement;
